package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "survivor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Survivor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "survivor_seq")
    @SequenceGenerator(name = "survivor_seq", sequenceName = "SURVIVOR_SEQ", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer survivorNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CurrentStatus currentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DetectionMethod detectionMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RescueStatus rescueStatus;

    @Column
    private LocalDateTime firstDetectedAt;

    @Column
    private LocalDateTime lastDetectedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFalsePositive = false;

    @Column
    private LocalDateTime falsePositiveReportedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
