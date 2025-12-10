package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import opensource.project.domain.Detection;

import java.time.LocalDateTime;

/**
 * WiFi 센서 탐지 기록 조회 API 응답 DTO
 * 프론트엔드의 그래프 초기화에 필요한 최소한의 정보만 포함함
 *
 * 사용처:
 * - GET /wifi-detections/sensor/{sensorId}/recent
 * - 페이지 로드 시 최근 N개 데이터를 가져와 그래프를 초기화함
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiDetectionRecordDto {

    /**
     * Detection ID
     */
    @JsonProperty("detection_id")
    private Long detectionId;

    /**
     * 생존자 탐지 여부
     * true인 경우 그래프에서 해당 포인트를 빨간색으로 표시함
     */
    @JsonProperty("survivor_detected")
    private Boolean survivorDetected;

    /**
     * 신호 강도 (RSSI, dBm 단위)
     * 그래프의 Y축 값으로 사용됨
     */
    @JsonProperty("signal_strength")
    private Integer signalStrength;

    /**
     * AI 모델의 탐지 신뢰도 (0.0 ~ 1.0)
     * 생존자가 탐지된 경우에만 유의미한 값을 가짐
     */
    @JsonProperty("confidence")
    private Double confidence;

    /**
     * 탐지 시각
     * 그래프의 X축 값으로 사용됨
     */
    @JsonProperty("detected_at")
    private LocalDateTime detectedAt;

    /**
     * 생존자 ID (탐지된 경우에만)
     * 생존자 상세 페이지로 이동하는 데 사용됨
     */
    @JsonProperty("survivor_id")
    private Long survivorId;

    /**
     * 생존자 번호 (탐지된 경우에만)
     * 예: "S-001", "S-042"
     * UI에 표시하기 위한 사용자 친화적인 번호
     */
    @JsonProperty("survivor_number")
    private String survivorNumber;

    /**
     * 센서 ID
     * 어떤 센서에서 탐지되었는지 확인하는 데 사용됨
     */
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * 위치 정보 (간략)
     * 예: "서울시 강남구 테헤란로 123"
     */
    @JsonProperty("location")
    private String location;

    /**
     * Detection 엔티티로부터 WifiDetectionRecordDto를 생성하는 정적 팩토리 메서드
     * Detection 엔티티를 API 응답용 DTO로 변환함
     *
     * @param detection Detection 엔티티
     * @return WifiDetectionRecordDto 인스턴스
     */
    public static WifiDetectionRecordDto from(Detection detection) {
        // 생존자 탐지 여부를 판단함 (Survivor가 연결되어 있으면 탐지된 것으로 간주)
        boolean survivorDetected = detection.getSurvivor() != null;

        WifiDetectionRecordDtoBuilder builder = WifiDetectionRecordDto.builder()
                .detectionId(detection.getId())
                .survivorDetected(survivorDetected)
                .signalStrength(detection.getSignalStrength())
                .confidence(detection.getConfidence())
                .detectedAt(detection.getDetectedAt())
                .sensorId(detection.getWifiSensor() != null ? detection.getWifiSensor().getSensorCode() : null)
                .location(detection.getLocation() != null ? detection.getLocation().getFullAddress() : null);

        // 생존자가 연결되어 있는 경우 생존자 정보를 추가함
        if (survivorDetected) {
            builder.survivorId(detection.getSurvivor().getId())
                    .survivorNumber(formatSurvivorNumber(detection.getSurvivor().getSurvivorNumber()));
        }

        return builder.build();
    }

    /**
     * 생존자 번호를 포맷팅함
     * 예: 1 → "S-001", 42 → "S-042"
     *
     * @param survivorNumber 생존자 번호
     * @return 포맷팅된 문자열
     */
    private static String formatSurvivorNumber(Integer survivorNumber) {
        if (survivorNumber == null) {
            return null;
        }
        return String.format("S-%03d", survivorNumber);
    }
}