package kz.adisker.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Base for all tenant-scoped entities.
 * organization_id is always required; branch_id is optional.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
}
