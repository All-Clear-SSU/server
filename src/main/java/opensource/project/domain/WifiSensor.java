package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;
import opensource.project.domain.enums.SensorStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wifi_sensor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wifi_sensor_seq")
    @SequenceGenerator(name = "wifi_sensor_seq", sequenceName = "WIFI_SENSOR_SEQ", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String sensorCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensorStatus status;

    @Column
    private Integer signalStrength;

    @Column
    private Double detectionRadius;

    @Column(nullable = false)
    private Boolean isActive;

    @Column
    private LocalDateTime lastActiveAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
