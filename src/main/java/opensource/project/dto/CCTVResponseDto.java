package opensource.project.dto;

import lombok.*;
import opensource.project.domain.CCTV;
import opensource.project.domain.enums.CCTVStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CCTVResponseDto {

    private Long id;
    private Integer cameraNumber;
    private String cctvCode;
    private CCTVStatus status;
    private Long locationId;
    private LocationResponseDto location;
    private Boolean isActive;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CCTVResponseDto from(CCTV cctv) {
        return CCTVResponseDto.builder()
                .id(cctv.getId())
                .cameraNumber(cctv.getCameraNumber())
                .cctvCode(cctv.getCctvCode())
                .status(cctv.getStatus())
                .locationId(cctv.getLocation().getId())
                .location(LocationResponseDto.from(cctv.getLocation()))
                .isActive(cctv.getIsActive())
                .lastActiveAt(cctv.getLastActiveAt())
                .createdAt(cctv.getCreatedAt())
                .updatedAt(cctv.getUpdatedAt())
                .build();
    }

    public static CCTVResponseDto fromWithoutLocation(CCTV cctv) {
        return CCTVResponseDto.builder()
                .id(cctv.getId())
                .cameraNumber(cctv.getCameraNumber())
                .cctvCode(cctv.getCctvCode())
                .status(cctv.getStatus())
                .locationId(cctv.getLocation().getId())
                .isActive(cctv.getIsActive())
                .lastActiveAt(cctv.getLastActiveAt())
                .createdAt(cctv.getCreatedAt())
                .updatedAt(cctv.getUpdatedAt())
                .build();
    }
}
