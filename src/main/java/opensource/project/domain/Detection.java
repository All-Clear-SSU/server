package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "detection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "detection_seq")
    @SequenceGenerator(name = "detection_seq", sequenceName = "DETECTION_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survivor_id", nullable = false)
    private Survivor survivor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DetectionType detectionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cctv_id")
    private CCTV cctv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wifi_sensor_id")
    private WifiSensor wifiSensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CurrentStatus detectedStatus;

    @Lob
    @Column
    private String aiAnalysisResult;

    @Column(length = 50)
    private String aiModelVersion;

    @Column
    private Double confidence;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String videoUrl;

    @Column
    private Integer signalStrength;

    @Lob
    @Column
    private String rawData;

    @Column
    private Integer fireCount;

    @Column
    private Integer humanCount;

    @Column
    private Integer smokeCount;

    @Column
    private Integer totalObjects;

    @Lob
    @Column
    private byte[] analyzedImage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
