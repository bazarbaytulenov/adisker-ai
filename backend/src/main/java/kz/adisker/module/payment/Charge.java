package kz.adisker.module.payment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Начисление за месяц по ребёнку (ТЗ 5.15, формулы 20.2).
 * Задолженность рассчитывается как amountCharged − оплачено − discount.
 */
@Entity
@Table(name = "charges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Charge {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;
    @Column(name = "child_id",        nullable = false) private UUID childId;
    @Column(name = "attendance_month_id") private UUID attendanceMonthId;

    @Column(nullable = false) private int year;
    @Column(nullable = false) private int month;

    @Column(name = "days_attended", nullable = false) private int daysAttended = 0;
    @Column(name = "days_absent",   nullable = false) private int daysAbsent = 0;

    @Column(name = "daily_rate")     private BigDecimal dailyRate;
    @Column(name = "amount_charged", nullable = false) private BigDecimal amountCharged = BigDecimal.ZERO;
    @Column(nullable = false)        private BigDecimal discount = BigDecimal.ZERO;
    @Column(name = "discount_reason", columnDefinition = "TEXT") private String discountReason;
    @Column(name = "balance_before", nullable = false) private BigDecimal balanceBefore = BigDecimal.ZERO;

    /** Уникальный идентификатор начисления (ТЗ 5.15). */
    @Column(name = "unique_charge_id", unique = true) private String uniqueChargeId;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
