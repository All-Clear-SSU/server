package opensource.project.dto;

import lombok.Builder;
import lombok.Getter;
import opensource.project.domain.RecentSurvivorRecord;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecentSurvivorRecordResponseDto {
    private Long id;
    private Long survivorId;
    private Integer survivorNumber;
    private String buildingName;
    private Integer floor;
    private String roomNumber;
    private String fullAddress;
    private LocalDateTime lastDetectedAt;
    private CurrentStatus lastPose;
    private Double lastRiskScore;
    private DetectionMethod detectionMethod;
    private Long cctvId;
    private Long wifiSensorId;
    private String aiAnalysisResult;
    private String aiSummary;
    private LocalDateTime archivedAt;

    public static RecentSurvivorRecordResponseDto from(RecentSurvivorRecord record) {
        return RecentSurvivorRecordResponseDto.builder()
                .id(record.getId())
                .survivorId(record.getSurvivorId())
                .survivorNumber(record.getSurvivorNumber())
                .buildingName(record.getBuildingName())
                .floor(record.getFloor())
                .roomNumber(record.getRoomNumber())
                .fullAddress(record.getFullAddress())
                .lastDetectedAt(record.getLastDetectedAt())
                .lastPose(record.getLastPose())
                .lastRiskScore(record.getLastRiskScore())
                .detectionMethod(record.getDetectionMethod())
                .cctvId(record.getCctvId())
                .wifiSensorId(record.getWifiSensorId())
                .aiAnalysisResult(record.getAiAnalysisResult())
                .aiSummary(record.getAiSummary())
                .archivedAt(record.getArchivedAt())
                .build();
    }
}
