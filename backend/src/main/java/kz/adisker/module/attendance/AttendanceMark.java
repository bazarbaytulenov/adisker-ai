package kz.adisker.module.attendance;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_marks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class AttendanceMark {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attendance_month_id", nullable = false) private UUID attendanceMonthId;
    @Column(name = "child_id", nullable = false)            private UUID childId;
    @Column(name = "organization_id", nullable = false)     private UUID organizationId;
    @Column(nullable = false)                               private int day;
    private String mark; // '1', 'б', 'о', or null

    @CreatedDate
    @Column(name = "created_at")  private Instant createdAt;
    @LastModifiedDate
    @Column(name = "updated_at")  private Instant updatedAt;
    @Column(name = "updated_by")  private UUID updatedBy;
}

