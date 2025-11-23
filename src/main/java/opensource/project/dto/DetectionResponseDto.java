package opensource.project.dto;

import lombok.*;
import opensource.project.domain.Detection;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionResponseDto {

    private Long id;
    private Long survivorId;
    private SurvivorResponseDto survivor;
    private DetectionType detectionType;
    private Long cctvId;
    private CCTVResponseDto cctv;
    private Long wifiSensorId;
    private WifiSensorResponseDto wifiSensor;
    private Long locationId;
    private LocationResponseDto location;
    private LocalDateTime detectedAt;
    private CurrentStatus detectedStatus;
    private String aiAnalysisResult;
    private String aiModelVersion;
    private Double confidence;
    private String imageUrl;
    private String videoUrl;
    private Integer signalStrength;
    private String rawData;
    private LocalDateTime createdAt;

    public static DetectionResponseDto from(Detection detection) {
        return DetectionResponseDto.builder()
                .id(detection.getId())
                .survivorId(detection.getSurvivor().getId())
                .survivor(SurvivorResponseDto.from(detection.getSurvivor()))
                .detectionType(detection.getDetectionType())
                .cctvId(detection.getCctv() != null ? detection.getCctv().getId() : null)
                .cctv(detection.getCctv() != null ? CCTVResponseDto.from(detection.getCctv()) : null)
                .wifiSensorId(detection.getWifiSensor() != null ? detection.getWifiSensor().getId() : null)
                .wifiSensor(detection.getWifiSensor() != null ? WifiSensorResponseDto.from(detection.getWifiSensor()) : null)
                .locationId(detection.getLocation().getId())
                .location(LocationResponseDto.from(detection.getLocation()))
                .detectedAt(detection.getDetectedAt())
                .detectedStatus(detection.getDetectedStatus())
                .aiAnalysisResult(detection.getAiAnalysisResult())
                .aiModelVersion(detection.getAiModelVersion())
                .confidence(detection.getConfidence())
                .imageUrl(detection.getImageUrl())
                .videoUrl(detection.getVideoUrl())
                .signalStrength(detection.getSignalStrength())
                .rawData(detection.getRawData())
                .createdAt(detection.getCreatedAt())
                .build();
    }

    public static DetectionResponseDto fromWithoutRelations(Detection detection) {
        return DetectionResponseDto.builder()
                .id(detection.getId())
                .survivorId(detection.getSurvivor().getId())
                .detectionType(detection.getDetectionType())
                .cctvId(detection.getCctv() != null ? detection.getCctv().getId() : null)
                .wifiSensorId(detection.getWifiSensor() != null ? detection.getWifiSensor().getId() : null)
                .locationId(detection.getLocation().getId())
                .detectedAt(detection.getDetectedAt())
                .detectedStatus(detection.getDetectedStatus())
                .aiAnalysisResult(detection.getAiAnalysisResult())
                .aiModelVersion(detection.getAiModelVersion())
                .confidence(detection.getConfidence())
                .imageUrl(detection.getImageUrl())
                .videoUrl(detection.getVideoUrl())
                .signalStrength(detection.getSignalStrength())
                .rawData(detection.getRawData())
                .createdAt(detection.getCreatedAt())
                .build();
    }
}
