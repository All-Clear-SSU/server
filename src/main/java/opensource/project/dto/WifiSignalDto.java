package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * WebSocket으로 브로드캐스트할 WiFi 센서 신호 데이터 DTO
 * 프론트엔드의 실시간 그래프 업데이트 및 생존자 탐지 알림에 사용됨
 *
 * 브로드캐스트 토픽: /topic/wifi-sensor/{sensorId}/signal
 * 발행 빈도: 5초마다 (생존자 탐지 여부와 무관)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiSignalDto {

    /**
     * WiFi 센서 ID
     * 예: "ESP32-001"
     */
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * 센서 이름 (사용자 친화적)
     * 예: "1층 로비 센서", "지하 주차장 센서"
     */
    @JsonProperty("sensor_name")
    private String sensorName;

    /**
     * 위치 ID
     */
    @JsonProperty("location_id")
    private Long locationId;

    /**
     * 위치 정보 (간략)
     * 예: "정보관 2층 01"
     */
    @JsonProperty("location_address")
    private String locationAddress;

    /**
     * 생존자 탐지 여부
     * true인 경우 프론트엔드에서 특수 효과 및 알림을 표시함
     */
    @JsonProperty("survivor_detected")
    private Boolean survivorDetected;

    /**
     * 생존자 ID (탐지된 경우에만)
     * 생존자가 탐지되고 DB에 저장된 경우 해당 Survivor의 ID를 포함함
     * 프론트엔드에서 생존자 상세 페이지로 이동하는 데 사용됨
     */
    @JsonProperty("survivor_id")
    private Long survivorId;

    /**
     * 생존자 번호 (탐지된 경우에만)
     * 예: "S-001", "S-042"
     */
    @JsonProperty("survivor_number")
    private String survivorNumber;

    /**
     * 신호 강도 (RSSI, dBm 단위)
     * 그래프의 주요 Y축 값으로 사용됨
     */
    @JsonProperty("signal_strength")
    private Integer signalStrength;

    /**
     * 탐지 신뢰도 (0.0 ~ 1.0)
     * 생존자 탐지 시 AI 모델의 신뢰도를 표시함
     */
    @JsonProperty("confidence")
    private Double confidence;

    /**
     * 메시지 생성 시각
     * 그래프의 X축 (시간축)에 사용됨
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * CSI 진폭 요약 데이터
     * 전체 CSI 데이터를 전송하면 너무 크므로, 그래프에 필요한 요약 데이터만 포함함
     * 각 부반송파의 평균 진폭 또는 대표값
     */
    @JsonProperty("csi_amplitude_summary")
    private java.util.List<Double> csiAmplitudeSummary;

    /**
     * 움직임 감지 여부
     * 그래프에 움직임 마커를 표시하는 데 사용됨
     */
    @JsonProperty("movement_detected")
    private Boolean movementDetected;

    /**
     * 움직임 강도 (0.0 ~ 1.0)
     * 그래프의 색상 강도나 마커 크기를 조절하는 데 사용됨
     */
    @JsonProperty("movement_intensity")
    private Double movementIntensity;

    /**
     * 호흡 감지 여부
     * 미세한 CSI 변화를 감지하여 호흡 존재를 확인함
     */
    @JsonProperty("breathing_detected")
    private Boolean breathingDetected;

    /**
     * 호흡률 (분당 호흡 횟수, BPM)
     * 생존자의 건강 상태를 추정하는 데 사용됨
     */
    @JsonProperty("breathing_rate")
    private Double breathingRate;

    /**
     * 센서 상태
     * 예: "ACTIVE", "LOW_BATTERY", "ERROR"
     * 프론트엔드에서 센서 아이콘 색상을 변경하는 데 사용됨
     */
    @JsonProperty("sensor_status")
    private String sensorStatus;

    /**
     * 배터리 잔량 (퍼센트, 0~100)
     * 센서 모니터링 대시보드에 표시됨
     */
    @JsonProperty("battery_level")
    private Integer batteryLevel;

    /**
     * 전체 CSI 분석 데이터 (선택적)
     * 생존자가 탐지된 경우에만 포함하여 프론트엔드에서 상세 그래프를 렌더링할 수 있게 함
     * 평상시에는 null로 설정하여 메시지 크기를 줄임
     */
    @JsonProperty("detailed_csi_analysis")
    private WifiAnalysisDataDto detailedCsiAnalysis;

    /**
     * MqttWifiDetectionDto로부터 WifiSignalDto를 생성하는 정적 팩토리 메서드
     * MQTT 메시지를 WebSocket 브로드캐스트용 DTO로 변환함
     *
     * @param mqttData MQTT로 수신한 원본 데이터
     * @param sensorName 센서 이름 (WifiSensor 엔티티에서 조회)
     * @param locationAddress 위치 주소 (Location 엔티티에서 조회)
     * @return WebSocket 브로드캐스트용 DTO
     */
    public static WifiSignalDto fromMqttData(MqttWifiDetectionDto mqttData,
                                              String sensorName,
                                              String locationAddress) {
        WifiSignalDtoBuilder builder = WifiSignalDto.builder()
                .sensorId(mqttData.getSensorId())
                .sensorName(sensorName)
                .locationId(mqttData.getLocationId())
                .locationAddress(locationAddress)
                .survivorDetected(mqttData.getSurvivorDetected())
                .signalStrength(mqttData.getSignalStrength())
                .confidence(mqttData.getConfidence())
                .timestamp(mqttData.getTimestamp())
                .batteryLevel(mqttData.getBatteryLevel())
                .sensorStatus(mqttData.getStatusMessage());

        // CSI 분석 데이터가 있는 경우 요약 정보 추출
        if (mqttData.getCsiAnalysis() != null) {
            WifiAnalysisDataDto csi = mqttData.getCsiAnalysis();

            builder.movementDetected(csi.getMovementDetected())
                    .movementIntensity(csi.getMovementIntensity())
                    .breathingDetected(csi.getBreathingDetected())
                    .breathingRate(csi.getBreathingRate());

            // CSI 진폭 요약: 각 부반송파의 최신 값을 추출함
            if (csi.getCsiAmplitude() != null && !csi.getCsiAmplitude().isEmpty()) {
                java.util.List<Double> summary = csi.getCsiAmplitude().stream()
                        .filter(subcarrierData -> !subcarrierData.isEmpty())
                        .map(subcarrierData -> subcarrierData.get(subcarrierData.size() - 1))
                        .collect(java.util.stream.Collectors.toList());
                builder.csiAmplitudeSummary(summary);
            }

            // 생존자가 탐지된 경우에만 상세 CSI 데이터를 포함함 (메시지 크기 최적화)
            if (Boolean.TRUE.equals(mqttData.getSurvivorDetected())) {
                builder.detailedCsiAnalysis(csi);
            }
        }

        return builder.build();
    }

    /**
     * 생존자 정보를 업데이트하는 메서드
     * 생존자 매칭 후 호출하여 생존자 ID와 번호를 설정함
     *
     * @param survivorId 생존자 ID
     * @param survivorNumber 생존자 번호
     */
    public void setSurvivorInfo(Long survivorId, String survivorNumber) {
        this.survivorId = survivorId;
        this.survivorNumber = survivorNumber;
    }
}