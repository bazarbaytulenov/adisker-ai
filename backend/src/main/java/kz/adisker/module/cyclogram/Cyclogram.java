package kz.adisker.module.cyclogram;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Циклограмма работы педагога на неделю.
 * Контент хранится как JSONB — список временных слотов по дням недели.
 */
@Entity
@Table(name = "cyclograms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Cyclogram extends TenantEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int week;

    @Column(nullable = false, length = 2)
    private String language = "ru";

    /**
     * JSONB: структурированный контент по дням.
     * Пример:
     * {
     *   "monday":    [{"time": "08:00-08:30", "activity": "Утренний приём"}],
     *   "tuesday":   [...],
     *   ...
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String content;

    @Column(name = "plan_section_id")
    private UUID planSectionId;

    @Column(name = "generated_by_ai", nullable = false)
    private boolean generatedByAi = false;

    @Column(nullable = false, length = 20)
    private String status = "draft"; // draft / approved
}
