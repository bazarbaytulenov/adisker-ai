package kz.adisker.module.plan;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "prospective_plan_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class PlanSection extends TenantEntity {
    @Column(name = "plan_id",       nullable = false) private UUID planId;
    @Column(nullable = false) private String domain; // kaz_language / music / physical / educator
    @Column(name = "domain_name_ru") private String domainNameRu;
    @Column(name = "domain_name_kk") private String domainNameKk;
    @Column(name = "owner_role")    private String ownerRole;
    @Column(name = "owner_user_id") private UUID ownerUserId;
    @Column(columnDefinition = "TEXT") private String content;
    @Column(columnDefinition = "TEXT") private String objectives;
    @Column(columnDefinition = "TEXT") private String materials;
    @Column(nullable = false) private String status = "draft";
    @Column(name = "submitted_at")  private Instant submittedAt;
    @Column(name = "approved_at")   private Instant approvedAt;
    @Column(name = "approved_by")   private UUID approvedBy;
    @Column(name = "returned_at")   private Instant returnedAt;
    @Column(name = "return_comment", columnDefinition = "TEXT") private String returnComment;
    @Column(nullable = false) private int version = 1;
    @Column(name = "sort_order")    private int sortOrder = 0;
    @Column(name = "updated_by")    private UUID updatedBy;
}
