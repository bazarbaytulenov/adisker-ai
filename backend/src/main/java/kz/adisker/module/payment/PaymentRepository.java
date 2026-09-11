package kz.adisker.module.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Payment> findByChildIdOrderByCreatedAtDesc(UUID childId);

    List<Payment> findByChargeId(UUID chargeId);

    List<Payment> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, String status);

    /** Сумма подтверждённых платежей по начислению. */
    @Query("""
           SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
           WHERE p.chargeId = :chargeId AND p.status = 'confirmed'
           """)
    BigDecimal sumConfirmedByCharge(@Param("chargeId") UUID chargeId);
}
