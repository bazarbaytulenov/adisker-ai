package kz.adisker.module.attendance;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_months")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class AttendanceMonth {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id", nullable = false)       private UUID branchId;
    @Column(name = "group_id", nullable = false)        private UUID groupId;
    @Column(nullable = false)                           private int year;
    @Column(nullable = false)                           private int month;
    @Column(name = "working_days")                      private Integer workingDays;
    @Column(name = "is_closed", nullable = false)       private boolean closed = false;
    @Column(name = "closed_by")                         private UUID closedBy;
    @Column(name = "closed_at")                         private Instant closedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at")                    private Instant updatedAt;
    @CreatedBy
    @Column(name = "created_by", updatable = false) private UUID createdBy;
}
