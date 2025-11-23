package opensource.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.RescueStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurvivorRequestDto {

    @NotNull(message = "생존자 번호는 필수입니다")
    private Integer survivorNumber;

    @NotNull(message = "위치 ID는 필수입니다")
    private Long locationId;

    @NotNull(message = "현재 상태는 필수입니다")
    private CurrentStatus currentStatus;

    @NotNull(message = "탐지 방법은 필수입니다")
    private DetectionMethod detectionMethod;

    @NotNull(message = "구조 상태는 필수입니다")
    private RescueStatus rescueStatus;

    private LocalDateTime firstDetectedAt;

    private LocalDateTime lastDetectedAt;

    private Boolean isActive;

    private Boolean isFalsePositive;

    private LocalDateTime falsePositiveReportedAt;
}
