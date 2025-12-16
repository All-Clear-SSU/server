package opensource.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import opensource.project.domain.enums.SensorStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiSensorRequestDto {

    @NotNull(message = "위치 ID는 필수입니다")
    private Long locationId;

    @NotNull(message = "상태는 필수입니다")
    private SensorStatus status;

    private String csiTopic;

    private Integer signalStrength;

    private Double detectionRadius;

    @NotNull(message = "활성 여부는 필수입니다")
    private Boolean isActive;
}
