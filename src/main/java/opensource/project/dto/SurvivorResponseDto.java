package opensource.project.dto;

import lombok.*;
import opensource.project.domain.Survivor;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurvivorResponseDto {

    private Long id;
    private Integer survivorNumber;
    private Long locationId;
    private LocationResponseDto location;
    private CurrentStatus currentStatus;
    private DetectionMethod detectionMethod;
    private RescueStatus rescueStatus;
    private LocalDateTime firstDetectedAt;
    private LocalDateTime lastDetectedAt;
    private Boolean isActive;
    private Boolean isFalsePositive;
    private LocalDateTime falsePositiveReportedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SurvivorResponseDto from(Survivor survivor) {
        return SurvivorResponseDto.builder()
                .id(survivor.getId())
                .survivorNumber(survivor.getSurvivorNumber())
                .locationId(survivor.getLocation().getId())
                .location(LocationResponseDto.from(survivor.getLocation()))
                .currentStatus(survivor.getCurrentStatus())
                .detectionMethod(survivor.getDetectionMethod())
                .rescueStatus(survivor.getRescueStatus())
                .firstDetectedAt(survivor.getFirstDetectedAt())
                .lastDetectedAt(survivor.getLastDetectedAt())
                .isActive(survivor.getIsActive())
                .isFalsePositive(survivor.getIsFalsePositive())
                .falsePositiveReportedAt(survivor.getFalsePositiveReportedAt())
                .createdAt(survivor.getCreatedAt())
                .updatedAt(survivor.getUpdatedAt())
                .build();
    }

    public static SurvivorResponseDto fromWithoutLocation(Survivor survivor) {
        return SurvivorResponseDto.builder()
                .id(survivor.getId())
                .survivorNumber(survivor.getSurvivorNumber())
                .locationId(survivor.getLocation().getId())
                .currentStatus(survivor.getCurrentStatus())
                .detectionMethod(survivor.getDetectionMethod())
                .rescueStatus(survivor.getRescueStatus())
                .firstDetectedAt(survivor.getFirstDetectedAt())
                .lastDetectedAt(survivor.getLastDetectedAt())
                .isActive(survivor.getIsActive())
                .isFalsePositive(survivor.getIsFalsePositive())
                .falsePositiveReportedAt(survivor.getFalsePositiveReportedAt())
                .createdAt(survivor.getCreatedAt())
                .updatedAt(survivor.getUpdatedAt())
                .build();
    }
}
