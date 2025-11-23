package opensource.project.dto;

import lombok.*;
import opensource.project.domain.Location;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponseDto {

    private Long id;
    private String buildingName;
    private Integer floor;
    private String roomNumber;
    private String fullAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LocationResponseDto from(Location location) {
        return LocationResponseDto.builder()
                .id(location.getId())
                .buildingName(location.getBuildingName())
                .floor(location.getFloor())
                .roomNumber(location.getRoomNumber())
                .fullAddress(location.getFullAddress())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
