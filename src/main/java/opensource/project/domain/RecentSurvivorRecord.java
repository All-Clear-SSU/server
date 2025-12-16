package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 타임아웃으로 제거된 생존자의 마지막 정보를 보존하는 스냅샷 엔티티.
 */
@Entity
@Table(name = "recent_survivor_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentSurvivorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recent_survivor_record_seq")
    @SequenceGenerator(name = "recent_survivor_record_seq", sequenceName = "RECENT_SURVIVOR_RECORD_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private Long survivorId;

    @Column(nullable = false)
    private Integer survivorNumber;

    @Column(length = 100)
    private String buildingName;

    @Column
    private Integer floor;

    @Column(length = 100)
    private String roomNumber;

    @Column(length = 255)
    private String fullAddress;

    @Column
    private LocalDateTime lastDetectedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CurrentStatus lastPose;

    @Column
    private Double lastRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DetectionMethod detectionMethod;

    @Column
    private Long cctvId;

    @Column
    private Long wifiSensorId;

    @Lob
    @Column
    private String aiAnalysisResult;

    @Column(length = 500)
    private String aiSummary;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime archivedAt;
}
