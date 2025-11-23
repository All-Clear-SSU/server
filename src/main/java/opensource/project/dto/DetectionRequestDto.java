package opensource.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionRequestDto {

    @NotNull(message = "생존자 ID는 필수입니다")
    private Long survivorId;

    @NotNull(message = "탐지 타입은 필수입니다")
    private DetectionType detectionType;

    // CCTV 탐지인 경우
    private Long cctvId;

    // WiFi 탐지인 경우
    private Long wifiSensorId;

    @NotNull(message = "위치 ID는 필수입니다")
    private Long locationId;

    @NotNull(message = "탐지 시각은 필수입니다")
    private LocalDateTime detectedAt;

    private CurrentStatus detectedStatus;

    private String aiAnalysisResult;

    private String aiModelVersion;

    private Double confidence;

    private String imageUrl;

    private String videoUrl;

    private Integer signalStrength;

    private String rawData;
}
