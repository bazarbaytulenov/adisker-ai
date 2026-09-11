package kz.adisker.module.payment;

import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.common.util.QrCodes;
import kz.adisker.module.attendance.AttendanceMark;
import kz.adisker.module.attendance.AttendanceMarkRepository;
import kz.adisker.module.attendance.AttendanceMonth;
import kz.adisker.module.attendance.AttendanceMonthRepository;
import kz.adisker.module.audit.AuditService;
import kz.adisker.module.child.Child;
import kz.adisker.module.child.ChildRepository;
import kz.adisker.module.organization.Organization;
import kz.adisker.module.organization.OrganizationRepository;
import kz.adisker.module.payment.PaymentDtos.*;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Начисления и платежи (ТЗ 5.15, формулы 20.2, A-08).
 * Задолженность = начислено − подтверждённые платежи − льготы.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final ChargeRepository chargeRepo;
    private final PaymentRepository paymentRepo;
    private final ChildRepository childRepo;
    private final AuditService auditService;
    private final AttendanceMonthRepository attendanceMonthRepo;
    private final AttendanceMarkRepository attendanceMarkRepo;
    private final OrganizationRepository organizationRepo;

    @Value("${payment.kaspi.pay-url-template:https://pay.kaspi.kz/pay?amount={amount}&reference={ref}}")
    private String kaspiUrlTemplate;

    // ── Начисления ──────────────────────────────────────────────────────────────

    @Transactional
    public ChargeDto createCharge(CreateChargeRequest req, UserPrincipal principal) {
        var child = childRepo
                .findByIdAndOrganizationIdAndDeletedFalse(req.getChildId(), principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Child", req.getChildId()));

        // Одно начисление на ребёнка за месяц
        chargeRepo.findByChildIdAndYearAndMonth(req.getChildId(), req.getYear(), req.getMonth())
                .ifPresent(c -> { throw new BusinessException("Начисление за этот месяц уже существует"); });

        int daysAttended = req.getDaysAttended() != null ? req.getDaysAttended() : 0;
        BigDecimal dailyRate = req.getDailyRate() != null ? req.getDailyRate() : BigDecimal.ZERO;

        // Сумма: явно заданная или рассчитанная как посещённые дни × ставка
        BigDecimal amount = req.getAmountCharged() != null
                ? req.getAmountCharged()
                : dailyRate.multiply(BigDecimal.valueOf(daysAttended));

        BigDecimal discount = req.getDiscount() != null ? req.getDiscount() : BigDecimal.ZERO;

        Charge charge = Charge.builder()
                .organizationId(principal.getOrganizationId())
                .branchId(child.getBranchId())
                .childId(child.getId())
                .year(req.getYear())
                .month(req.getMonth())
                .daysAttended(daysAttended)
                .daysAbsent(req.getDaysAbsent() != null ? req.getDaysAbsent() : 0)
                .dailyRate(dailyRate)
                .amountCharged(amount)
                .discount(discount)
                .discountReason(req.getDiscountReason())
                .balanceBefore(BigDecimal.ZERO)
                .createdBy(principal.getId())
                .build();
        charge.setUniqueChargeId(buildUniqueId(child.getId(), req.getYear(), req.getMonth()));

        Charge saved = chargeRepo.save(charge);
        auditService.record("CREATE", "charge", saved.getId(), null, null,
                "Начисление " + amount + " за " + req.getMonth() + "/" + req.getYear());
        return toChargeDto(saved);
    }

    /**
     * Автоматическая генерация начислений из закрытого табеля посещаемости
     * (ТЗ 5.14→5.15). По каждому ребёнку группы: посещённые дни ("1") × тариф
     * организации. Уже существующие за месяц начисления пропускаются.
     *
     * @return сводка: сколько создано и сколько пропущено
     */
    @Transactional
    public Map<String, Object> generateFromAttendance(UUID groupId, int year, int month,
                                                      UserPrincipal principal) {
        AttendanceMonth am = attendanceMonthRepo.findByGroupIdAndYearAndMonth(groupId, year, month)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceMonth", groupId));

        if (!am.getOrganizationId().equals(principal.getOrganizationId())) {
            throw new BusinessException("Табель принадлежит другой организации");
        }
        if (!am.isClosed()) {
            throw new BusinessException("Табель за месяц не закрыт. Закройте табель перед начислением.");
        }

        Organization org = organizationRepo.findByIdAndDeletedFalse(principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", principal.getOrganizationId()));
        BigDecimal dailyRate = org.getDailyRate() != null ? org.getDailyRate() : BigDecimal.ZERO;
        if (dailyRate.signum() <= 0) {
            throw new BusinessException("Не задан тариф (дневная ставка) организации");
        }

        // Отметки табеля по детям: считаем дни присутствия ("1")
        List<AttendanceMark> marks = attendanceMarkRepo.findByAttendanceMonthId(am.getId());
        Map<UUID, Integer> attendedByChild = new java.util.HashMap<>();
        Map<UUID, Integer> absentByChild = new java.util.HashMap<>();
        for (AttendanceMark m : marks) {
            if ("1".equals(m.getMark())) {
                attendedByChild.merge(m.getChildId(), 1, Integer::sum);
            } else if (m.getMark() != null && !m.getMark().isBlank()) {
                // б (больничный), о (отпуск) — считаем как пропуски (не оплачиваются)
                absentByChild.merge(m.getChildId(), 1, Integer::sum);
            }
        }

        int created = 0, skipped = 0;
        for (Map.Entry<UUID, Integer> e : attendedByChild.entrySet()) {
            UUID childId = e.getKey();
            int daysAttended = e.getValue();

            // одно начисление на ребёнка за месяц
            if (chargeRepo.findByChildIdAndYearAndMonth(childId, year, month).isPresent()) {
                skipped++;
                continue;
            }
            Child child = childRepo.findByIdAndOrganizationIdAndDeletedFalse(childId,
                    principal.getOrganizationId()).orElse(null);
            if (child == null) { skipped++; continue; }

            BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(daysAttended));
            // Индивидуальная льгота ребёнка: скидка = сумма × процент / 100
            BigDecimal benefitPct = child.getBenefitPercent() != null
                    ? child.getBenefitPercent() : BigDecimal.ZERO;
            BigDecimal discount = amount.multiply(benefitPct)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            Charge charge = Charge.builder()
                    .organizationId(principal.getOrganizationId())
                    .branchId(child.getBranchId()).childId(childId)
                    .attendanceMonthId(am.getId())
                    .year(year).month(month)
                    .daysAttended(daysAttended)
                    .daysAbsent(absentByChild.getOrDefault(childId, 0))
                    .dailyRate(dailyRate).amountCharged(amount)
                    .discount(discount)
                    .discountReason(benefitPct.signum() > 0 ? child.getBenefitReason() : null)
                    .balanceBefore(BigDecimal.ZERO)
                    .createdBy(principal.getId())
                    .build();
            charge.setUniqueChargeId(buildUniqueId(childId, year, month));
            chargeRepo.save(charge);
            created++;
        }

        auditService.record("CREATE", "charge_batch", am.getId(), null, null,
                "Авто-начисления из табеля " + month + "/" + year + ": создано " + created
                        + ", пропущено " + skipped);
        return Map.of("created", created, "skipped", skipped, "dailyRate", dailyRate);
    }

    public List<ChargeDto> listChargesByChild(UUID childId, UserPrincipal principal) {
        assertChildInOrg(childId, principal);
        return chargeRepo.findByChildIdOrderByYearDescMonthDesc(childId)
                .stream().map(this::toChargeDto).toList();
    }

    public ChargeDto getCharge(UUID chargeId, UserPrincipal principal) {
        return toChargeDto(findCharge(chargeId, principal));
    }

    // ── Kaspi: ссылка/QR для оплаты ───────────────────────────────────────────────

    public KaspiPaymentDto kaspiPaymentData(UUID chargeId, UserPrincipal principal) {
        Charge charge = findCharge(chargeId, principal);
        BigDecimal debt = debtOf(charge);
        if (debt.signum() <= 0) {
            throw new BusinessException("По этому начислению нет задолженности");
        }
        String url = kaspiUrlTemplate
                .replace("{amount}", debt.toPlainString())
                .replace("{ref}", charge.getUniqueChargeId());
        return KaspiPaymentDto.builder()
                .chargeId(charge.getId())
                .amount(debt)
                .uniqueChargeId(charge.getUniqueChargeId())
                .paymentUrl(url)
                .qrUrl(QrCodes.toDataUrl(url, 240))
                .build();
    }

    // ── Платежи ───────────────────────────────────────────────────────────────

    /** Создание платежа (родитель оплатил по QR/ссылке — статус pending до подтверждения). */
    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest req, UserPrincipal principal) {
        Charge charge = findCharge(req.getChargeId(), principal);

        Payment payment = Payment.builder()
                .organizationId(charge.getOrganizationId())
                .childId(charge.getChildId())
                .chargeId(charge.getId())
                .amount(req.getAmount())
                .paymentMethod(req.getPaymentMethod())
                .paymentDate(LocalDate.now())
                .kaspiTxnId(req.getKaspiTxnId())
                .status("pending")
                .notes(req.getNotes())
                .createdBy(principal.getId())
                .build();
        Payment saved = paymentRepo.save(payment);
        auditService.record("CREATE", "payment", saved.getId(), null, null,
                "Платёж " + req.getAmount() + " (ожидает подтверждения)");
        return toPaymentDto(saved);
    }

    /** Подтверждение платежа бухгалтером (ТЗ 5.15, A-08). */
    @Transactional
    public PaymentDto confirmPayment(UUID paymentId, UserPrincipal principal) {
        Payment payment = paymentRepo
                .findByIdAndOrganizationId(paymentId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if ("confirmed".equals(payment.getStatus())) {
            throw new BusinessException("Платёж уже подтверждён");
        }
        if ("cancelled".equals(payment.getStatus())) {
            throw new BusinessException("Отменённый платёж нельзя подтвердить");
        }
        payment.setStatus("confirmed");
        payment.setConfirmedBy(principal.getId());
        payment.setConfirmedAt(Instant.now());
        Payment saved = paymentRepo.save(payment);
        auditService.record("UPDATE", "payment", saved.getId(), null, null,
                "Платёж подтверждён бухгалтером: " + saved.getAmount());
        return toPaymentDto(saved);
    }

    @Transactional
    public PaymentDto cancelPayment(UUID paymentId, UserPrincipal principal) {
        Payment payment = paymentRepo
                .findByIdAndOrganizationId(paymentId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if ("confirmed".equals(payment.getStatus())) {
            throw new BusinessException("Подтверждённый платёж нельзя отменить");
        }
        payment.setStatus("cancelled");
        Payment saved = paymentRepo.save(payment);
        auditService.record("UPDATE", "payment", saved.getId(), null, null, "Платёж отменён");
        return toPaymentDto(saved);
    }

    /** Реестр платежей организации по статусу (для бухгалтера). */
    public List<PaymentDto> registry(String status, UserPrincipal principal) {
        String st = (status == null || status.isBlank()) ? "pending" : status;
        return paymentRepo.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                        principal.getOrganizationId(), st)
                .stream().map(this::toPaymentDto).toList();
    }

    public List<PaymentDto> listPaymentsByChild(UUID childId, UserPrincipal principal) {
        assertChildInOrg(childId, principal);
        return paymentRepo.findByChildIdOrderByCreatedAtDesc(childId)
                .stream().map(this::toPaymentDto).toList();
    }

    // ── Формулы и helpers ─────────────────────────────────────────────────────────

    /** Задолженность = начислено − подтверждённые платежи − льготы (min 0). */
    private BigDecimal debtOf(Charge c) {
        BigDecimal paid = paymentRepo.sumConfirmedByCharge(c.getId());
        BigDecimal debt = c.getAmountCharged().subtract(paid).subtract(c.getDiscount());
        return debt.signum() > 0 ? debt : BigDecimal.ZERO;
    }

    /** Переплата = max(0, оплачено − (начислено − льготы)). */
    private BigDecimal overpaymentOf(Charge c) {
        BigDecimal paid = paymentRepo.sumConfirmedByCharge(c.getId());
        BigDecimal net = c.getAmountCharged().subtract(c.getDiscount());
        BigDecimal over = paid.subtract(net);
        return over.signum() > 0 ? over : BigDecimal.ZERO;
    }

    private Charge findCharge(UUID chargeId, UserPrincipal principal) {
        return chargeRepo.findByIdAndOrganizationId(chargeId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Charge", chargeId));
    }

    private void assertChildInOrg(UUID childId, UserPrincipal principal) {
        childRepo.findByIdAndOrganizationIdAndDeletedFalse(childId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Child", childId));
    }

    private String buildUniqueId(UUID childId, int year, int month) {
        return String.format("CHG-%d%02d-%s", year, month,
                childId.toString().substring(0, 8).toUpperCase());
    }

    private ChargeDto toChargeDto(Charge c) {
        BigDecimal paid = paymentRepo.sumConfirmedByCharge(c.getId());
        return ChargeDto.builder()
                .id(c.getId()).childId(c.getChildId())
                .year(c.getYear()).month(c.getMonth())
                .daysAttended(c.getDaysAttended()).daysAbsent(c.getDaysAbsent())
                .dailyRate(c.getDailyRate())
                .amountCharged(c.getAmountCharged())
                .discount(c.getDiscount())
                .paidConfirmed(paid)
                .debt(debtOf(c))
                .overpayment(overpaymentOf(c))
                .uniqueChargeId(c.getUniqueChargeId())
                .build();
    }

    private PaymentDto toPaymentDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId()).childId(p.getChildId()).chargeId(p.getChargeId())
                .amount(p.getAmount()).paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .confirmedBy(p.getConfirmedBy()).confirmedAt(p.getConfirmedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
