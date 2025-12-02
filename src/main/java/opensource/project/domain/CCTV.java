package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;
import opensource.project.domain.enums.CCTVStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cctv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CCTV {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cctv_seq")
    @SequenceGenerator(name = "cctv_seq", sequenceName = "CCTV_SEQ", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private Integer cameraNumber;

    @Column(unique = true, nullable = false, length = 50)
    private String cctvCode;

    @Column(length = 100)
    private String cctvName;

    @Column(length = 255)
    private String rtspUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CCTVStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

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
