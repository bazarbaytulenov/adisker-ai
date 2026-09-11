package kz.adisker.module.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Платёж по начислению (ТЗ 5.15, A-08).
 * Статусы: pending (ожидает подтверждения) / confirmed (подтверждён бухгалтером) / cancelled.
 */
@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "child_id",        nullable = false) private UUID childId;
    @Column(name = "charge_id")       private UUID chargeId;

    @Column(nullable = false) private BigDecimal amount;

    /** kaspi_qr / kaspi_link / cash / transfer */
    @Column(name = "payment_method") private String paymentMethod;
    @Column(name = "payment_date")   private LocalDate paymentDate;
    @Column(name = "kaspi_txn_id")   private String kaspiTxnId;

    /** pending / confirmed / cancelled */
    @Column(nullable = false) private String status = "pending";

    @Column(name = "confirmed_by") private UUID confirmedBy;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(columnDefinition = "TEXT") private String notes;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "created_by") private UUID createdBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
