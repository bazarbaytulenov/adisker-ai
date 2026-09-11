package kz.adisker.module.parent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/** DTO и Request-объекты модуля родителей. */
public class ParentDtos {

    /** Запрос на создание приглашения (сотрудником). */
    @Data
    public static class CreateInvitationRequest {
        @NotNull private UUID childId;
        private String phone;          // для WhatsApp-ссылки и автоподстановки
        private Integer expiresInDays; // опционально; по умолчанию 30
    }

    /** Ответ с данными приглашения: код, ссылка, QR, WhatsApp. */
    @Data @Builder
    public static class InvitationDto {
        private UUID id;
        private UUID childId;
        private String childFullName;
        private String inviteCode;
        private String phone;
        private String status;
        private Instant expiresAt;
        private Instant createdAt;
        private String inviteLink;     // ссылка регистрации
        private String qrUrl;          // data-URL PNG QR-кода
        private String whatsappLink;   // ссылка отправки через WhatsApp
    }

    /** Публичный запрос регистрации родителя по коду приглашения. */
    @Data
    public static class RegisterParentRequest {
        @NotBlank private String inviteCode;
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        private String middleName;
        @NotBlank private String email;
        private String phone;
        @NotBlank private String password;
        private boolean consentGiven; // согласие на обработку ПДн
    }

    /** Краткая инфа о ребёнке для кабинета родителя. */
    @Data @Builder
    public static class ChildCardDto {
        private UUID childId;
        private String fullName;
        private String groupId;
        private String status;
        private String photoUrl;
    }
}
