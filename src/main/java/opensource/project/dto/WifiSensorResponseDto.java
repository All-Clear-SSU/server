package opensource.project.dto;

import lombok.*;
import opensource.project.domain.WifiSensor;
import opensource.project.domain.enums.SensorStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiSensorResponseDto {

    private Long id;
    private String sensorCode;
    private Long locationId;
    private LocationResponseDto location;
    private SensorStatus status;
    private Integer signalStrength;
    private Double detectionRadius;
    private Boolean isActive;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WifiSensorResponseDto from(WifiSensor wifiSensor) {
        return WifiSensorResponseDto.builder()
                .id(wifiSensor.getId())
                .sensorCode(wifiSensor.getSensorCode())
                .locationId(wifiSensor.getLocation().getId())
                .location(LocationResponseDto.from(wifiSensor.getLocation()))
                .status(wifiSensor.getStatus())
                .signalStrength(wifiSensor.getSignalStrength())
                .detectionRadius(wifiSensor.getDetectionRadius())
                .isActive(wifiSensor.getIsActive())
                .lastActiveAt(wifiSensor.getLastActiveAt())
                .createdAt(wifiSensor.getCreatedAt())
                .updatedAt(wifiSensor.getUpdatedAt())
                .build();
    }

    public static WifiSensorResponseDto fromWithoutLocation(WifiSensor wifiSensor) {
        return WifiSensorResponseDto.builder()
                .id(wifiSensor.getId())
                .sensorCode(wifiSensor.getSensorCode())
                .locationId(wifiSensor.getLocation().getId())
                .status(wifiSensor.getStatus())
                .signalStrength(wifiSensor.getSignalStrength())
                .detectionRadius(wifiSensor.getDetectionRadius())
                .isActive(wifiSensor.getIsActive())
                .lastActiveAt(wifiSensor.getLastActiveAt())
                .createdAt(wifiSensor.getCreatedAt())
                .updatedAt(wifiSensor.getUpdatedAt())
                .build();
    }
}
