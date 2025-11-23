package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;
import opensource.project.domain.enums.UrgencyLevel;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "priority_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "priority_assessment_seq")
    @SequenceGenerator(name = "priority_assessment_seq", sequenceName = "PRIORITY_ASSESSMENT_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survivor_id", nullable = false)
    private Survivor survivor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detection_id", nullable = false)
    private Detection detection;

    @Column(nullable = false)
    private LocalDateTime assessedAt;

    @Column(nullable = false)
    private Double statusScore;

    @Column(nullable = false)
    private Double environmentScore;

    @Column(nullable = false)
    private Double confidenceCoefficient;

    @Column(nullable = false)
    private Double finalRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UrgencyLevel urgencyLevel;

    @Column(length = 500)
    private String calculationFormula;

    @Column(length = 50)
    private String aiModelVersion;

    @Lob
    @Column
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    public void calculateUrgencyLevel() {
        if (finalRiskScore != null) {
            if (finalRiskScore >= 8.0) {
                this.urgencyLevel = UrgencyLevel.CRITICAL;
            } else if (finalRiskScore >= 6.0) {
                this.urgencyLevel = UrgencyLevel.HIGH;
            } else if (finalRiskScore >= 4.0) {
                this.urgencyLevel = UrgencyLevel.MEDIUM;
            } else {
                this.urgencyLevel = UrgencyLevel.LOW;
            }
        }
    }
}
