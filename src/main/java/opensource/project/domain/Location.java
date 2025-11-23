package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "location")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_seq")
    @SequenceGenerator(name = "location_seq", sequenceName = "LOCATION_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 100)
    private String buildingName;

    @Column(nullable = false)
    private Integer floor;

    @Column(length = 50)
    private String roomNumber;

    @Column(length = 255)
    private String fullAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void generateFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (buildingName != null) {
            sb.append(buildingName);
        }
        if (floor != null) {
            sb.append(" ").append(floor).append("층");
        }
        if (roomNumber != null && !roomNumber.isEmpty()) {
            sb.append(" ").append(roomNumber);
        }
        this.fullAddress = sb.toString().trim();
    }
}
