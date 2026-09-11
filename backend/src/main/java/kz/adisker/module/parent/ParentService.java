package kz.adisker.module.parent;

import kz.adisker.common.RoleCode;
import kz.adisker.common.exception.AccessDeniedException;
import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.module.child.Child;
import kz.adisker.module.child.ChildRepository;
import kz.adisker.module.parent.ParentDtos.*;
import kz.adisker.module.user.User;
import kz.adisker.module.user.UserRepository;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Приглашения и регистрация родителей (ТЗ 5.20, 5.21, 3.1, A-07).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    private final ParentInvitationRepository invitationRepo;
    private final ParentAccountRepository accountRepo;
    private final ChildRepository childRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${app.invite-base-url:http://localhost:3000/invite}")
    private String inviteBaseUrl;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // без похожих символов
    private static final int CODE_LENGTH = 8;
    private static final int DEFAULT_EXPIRY_DAYS = 30;

    // ── Создание приглашения (сотрудником) ──────────────────────────────────────

    @Transactional
    public InvitationDto createInvitation(CreateInvitationRequest req, UserPrincipal principal) {
        Child child = childRepo
                .findByIdAndOrganizationIdAndDeletedFalse(req.getChildId(), principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Child", req.getChildId()));

        int expiryDays = req.getExpiresInDays() != null && req.getExpiresInDays() > 0
                ? req.getExpiresInDays() : DEFAULT_EXPIRY_DAYS;

        ParentInvitation inv = invitationRepo.save(ParentInvitation.builder()
                .organizationId(principal.getOrganizationId())
                .childId(child.getId())
                .inviteCode(generateUniqueCode())
                .phone(req.getPhone())
                .createdBy(principal.getId())
                .status("active")
                .expiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS))
                .build());

        auditService.record("CREATE", "parent_invitation", inv.getId(), null, null,
                "Создано приглашение родителя для ребёнка " + fullName(child));

        return toDto(inv, child);
    }

    public List<InvitationDto> listInvitations(UserPrincipal principal) {
        return invitationRepo.findByOrganizationIdOrderByCreatedAtDesc(principal.getOrganizationId())
                .stream()
                .map(inv -> toDto(inv, childRepo.findById(inv.getChildId()).orElse(null)))
                .toList();
    }

    @Transactional
    public void revokeInvitation(UUID invitationId, UserPrincipal principal) {
        ParentInvitation inv = invitationRepo
                .findByIdAndOrganizationId(invitationId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("ParentInvitation", invitationId));
        if ("used".equals(inv.getStatus())) {
            throw new BusinessException("Использованное приглашение нельзя отозвать");
        }
        inv.setStatus("revoked");
        inv.setRevokedAt(Instant.now());
        invitationRepo.save(inv);
        auditService.record("UPDATE", "parent_invitation", inv.getId(), null, null,
                "Приглашение отозвано");
    }

    // ── Регистрация родителя (публично, по коду) ────────────────────────────────

    @Transactional
    public UUID registerParent(RegisterParentRequest req) {
        if (!req.isConsentGiven()) {
            throw new BusinessException("Требуется согласие на обработку персональных данных");
        }

        ParentInvitation inv = invitationRepo.findByInviteCode(req.getInviteCode().trim())
                .orElseThrow(() -> new BusinessException("Приглашение не найдено"));

        // Однократность и срок действия
        if (!"active".equals(inv.getStatus())) {
            throw new BusinessException("Приглашение уже использовано или отозвано");
        }
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Срок действия приглашения истёк");
        }

        String email = req.getEmail().trim().toLowerCase();
        if (userRepo.existsByEmail(email)) {
            throw new BusinessException("Пользователь с таким email уже существует");
        }

        // Создаём аккаунт родителя (user с ролью PARENT)
        User parent = User.builder()
                .email(email)
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .middleName(req.getMiddleName())
                .roleCode(RoleCode.PARENT)
                .active(true)
                .preferredLanguage("ru")
                .build();
        parent.setOrganizationId(inv.getOrganizationId());
        parent = userRepo.save(parent);

        // Привязка к ребёнку из приглашения (только к одному)
        accountRepo.save(ParentAccount.builder()
                .userId(parent.getId())
                .childId(inv.getChildId())
                .organizationId(inv.getOrganizationId())
                .invitationId(inv.getId())
                .consentGiven(true)
                .consentDate(Instant.now())
                .active(true)
                .build());

        // Помечаем приглашение использованным
        inv.setStatus("used");
        inv.setUsedAt(Instant.now());
        invitationRepo.save(inv);

        auditService.recordAuth("CREATE", parent.getId(), inv.getOrganizationId(), email,
                "Регистрация родителя по приглашению");

        return parent.getId();
    }

    // ── Кабинет родителя ────────────────────────────────────────────────────────

    /** Список детей, к которым привязан родитель. */
    public List<ChildCardDto> myChildren(UserPrincipal principal) {
        requireParent(principal);
        return accountRepo.findByUserIdAndActiveTrue(principal.getId()).stream()
                .map(acc -> childRepo.findById(acc.getChildId()).orElse(null))
                .filter(c -> c != null)
                .map(this::toChildCard)
                .toList();
    }

    /**
     * Проверка, что родитель имеет доступ к указанному ребёнку.
     * Используется другими модулями кабинета родителя.
     */
    public void assertParentOwnsChild(UUID childId, UserPrincipal principal) {
        requireParent(principal);
        if (!accountRepo.existsByUserIdAndChildId(principal.getId(), childId)) {
            throw new AccessDeniedException("Родителю доступны только данные своего ребёнка");
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private void requireParent(UserPrincipal principal) {
        if (!RoleCode.PARENT.equals(principal.getRoleCode())) {
            throw new AccessDeniedException("Доступно только для роли родителя");
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!invitationRepo.existsByInviteCode(code)) return code;
        }
        throw new BusinessException("Не удалось сгенерировать код приглашения");
    }

    private InvitationDto toDto(ParentInvitation inv, Child child) {
        String link = inviteBaseUrl + (inviteBaseUrl.contains("?") ? "&" : "/") + inv.getInviteCode();
        String qr = kz.adisker.common.util.QrCodes.toDataUrl(link, 240);
        String whatsapp = buildWhatsappLink(inv.getPhone(), link, child);
        return InvitationDto.builder()
                .id(inv.getId())
                .childId(inv.getChildId())
                .childFullName(child != null ? fullName(child) : null)
                .inviteCode(inv.getInviteCode())
                .phone(inv.getPhone())
                .status(inv.getStatus())
                .expiresAt(inv.getExpiresAt())
                .createdAt(inv.getCreatedAt())
                .inviteLink(link)
                .qrUrl(qr)
                .whatsappLink(whatsapp)
                .build();
    }

    private String buildWhatsappLink(String phone, String link, Child child) {
        String childName = child != null ? fullName(child) : "вашего ребёнка";
        String text = "Здравствуйте! Приглашаем вас в систему детского сада для " + childName
                + ". Регистрация по ссылке: " + link;
        // URLEncoder кодирует пробел как '+', а wa.me ожидает URI-компонент (RFC 3986),
        // где пробел = %20. Иначе WhatsApp некорректно раскрывает текст с кириллицей.
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
        String cleanPhone = phone != null ? phone.replaceAll("[^0-9]", "") : "";
        return cleanPhone.isEmpty()
                ? "https://wa.me/?text=" + encoded
                : "https://wa.me/" + cleanPhone + "?text=" + encoded;
    }

    private ChildCardDto toChildCard(Child c) {
        return ChildCardDto.builder()
                .childId(c.getId())
                .fullName(fullName(c))
                .groupId(c.getGroupId() != null ? c.getGroupId().toString() : null)
                .status(c.getStatus())
                .photoUrl(c.getPhotoUrl())
                .build();
    }

    private String fullName(Child c) {
        return (c.getLastName() + " " + c.getFirstName()
                + (c.getMiddleName() != null ? " " + c.getMiddleName() : ""))
                .trim().replaceAll("\\s+", " ");
    }
}
