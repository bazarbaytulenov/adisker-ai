package kz.adisker.module.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO и Request-объекты модуля платежей. */
public class PaymentDtos {

    /** Запрос на создание начисления (бухгалтер/директор). */
    @Data
    public static class CreateChargeRequest {
        @NotNull private UUID childId;
        @NotNull private Integer year;
        @NotNull private Integer month;
        private Integer daysAttended;
        private Integer daysAbsent;
        private BigDecimal dailyRate;
        private BigDecimal amountCharged;   // если задан — используется напрямую
        private BigDecimal discount;
        private String discountReason;
    }

    /** Начисление с рассчитанной задолженностью. */
    @Data @Builder
    public static class ChargeDto {
        private UUID id;
        private UUID childId;
        private int year;
        private int month;
        private int daysAttended;
        private int daysAbsent;
        private BigDecimal dailyRate;
        private BigDecimal amountCharged;
        private BigDecimal discount;
        private BigDecimal paidConfirmed;   // сумма подтверждённых платежей
        private BigDecimal debt;            // задолженность = charged − paid − discount
        private BigDecimal overpayment;     // переплата (если paid > charged − discount)
        private String uniqueChargeId;
    }

    /** Запрос на создание платежа (родитель/бухгалтер). */
    @Data
    public static class CreatePaymentRequest {
        @NotNull private UUID chargeId;
        @NotNull @Positive private BigDecimal amount;
        private String paymentMethod;   // kaspi_qr / kaspi_link / cash / transfer
        private String kaspiTxnId;
        private String notes;
    }

    /** Платёж. */
    @Data @Builder
    public static class PaymentDto {
        private UUID id;
        private UUID childId;
        private UUID chargeId;
        private BigDecimal amount;
        private String paymentMethod;
        private String status;
        private UUID confirmedBy;
        private Instant confirmedAt;
        private Instant createdAt;
    }

    /** Данные для оплаты по Kaspi (ссылка + QR). */
    @Data @Builder
    public static class KaspiPaymentDto {
        private UUID chargeId;
        private BigDecimal amount;
        private String uniqueChargeId;
        private String paymentUrl;
        private String qrUrl;
    }
}
