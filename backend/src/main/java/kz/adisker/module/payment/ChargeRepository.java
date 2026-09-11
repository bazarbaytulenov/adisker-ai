package kz.adisker.module.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChargeRepository extends JpaRepository<Charge, UUID> {

    Optional<Charge> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Charge> findByChildIdOrderByYearDescMonthDesc(UUID childId);

    Optional<Charge> findByChildIdAndYearAndMonth(UUID childId, int year, int month);

    List<Charge> findByOrganizationIdAndYearAndMonth(UUID organizationId, int year, int month);
}
