package kz.adisker.module.payment;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.payment.PaymentDtos.*;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Payments")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    // ── Начисления ──────────────────────────────────────────────────────────────

    /** POST /api/payments/charges — создать начисление. */
    @PostMapping("/charges")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<ChargeDto> createCharge(@Valid @RequestBody CreateChargeRequest req,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Начисление создано", service.createCharge(req, principal));
    }

    /** POST /api/payments/charges/generate?groupId=&year=&month= — авто-начисления из табеля. */
    @PostMapping("/charges/generate")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<java.util.Map<String, Object>> generate(
            @RequestParam UUID organizationId, @RequestParam UUID groupId,
            @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = service.generateFromAttendance(groupId, year, month, principal);
        return ApiResponse.ok("Начисления сформированы", result);
    }

    /** GET /api/payments/charges?childId= — начисления ребёнка. */
    @GetMapping("/charges")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','DIRECTOR','METHODIST','FOUNDER','SYSTEM_ADMIN','PARENT')")
    public ApiResponse<List<ChargeDto>> chargesByChild(@RequestParam UUID childId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.listChargesByChild(childId, principal));
    }

    /** GET /api/payments/charges/{id}/kaspi — данные для оплаты по Kaspi (ссылка + QR). */
    @GetMapping("/charges/{id}/kaspi")
    @PreAuthorize("hasAnyRole('PARENT','ACCOUNTANT','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<KaspiPaymentDto> kaspi(@PathVariable("id") UUID chargeId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.kaspiPaymentData(chargeId, principal));
    }

    // ── Платежи ───────────────────────────────────────────────────────────────

    /** POST /api/payments — зарегистрировать платёж (родитель/бухгалтер). */
    @PostMapping
    @PreAuthorize("hasAnyRole('PARENT','ACCOUNTANT','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<PaymentDto> pay(@Valid @RequestBody CreatePaymentRequest req,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Платёж зарегистрирован", service.createPayment(req, principal));
    }

    /** PATCH /api/payments/{id}/confirm — подтвердить платёж (бухгалтер). */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<PaymentDto> confirm(@PathVariable UUID id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Платёж подтверждён", service.confirmPayment(id, principal));
    }

    /** PATCH /api/payments/{id}/cancel — отменить платёж. */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<PaymentDto> cancel(@PathVariable UUID id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Платёж отменён", service.cancelPayment(id, principal));
    }

    /** GET /api/payments/registry?status= — реестр платежей (бухгалтер). */
    @GetMapping("/registry")
    @PreAuthorize("hasAnyRole('ACCOUNTANT','DIRECTOR','FOUNDER','SYSTEM_ADMIN')")
    public ApiResponse<List<PaymentDto>> registry(@RequestParam(required = false) String status,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.registry(status, principal));
    }

    /** GET /api/payments?childId= — платежи ребёнка. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT','DIRECTOR','METHODIST','FOUNDER','SYSTEM_ADMIN','PARENT')")
    public ApiResponse<List<PaymentDto>> byChild(@RequestParam UUID childId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.listPaymentsByChild(childId, principal));
    }
}
