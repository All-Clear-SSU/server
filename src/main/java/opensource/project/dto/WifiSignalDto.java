package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebSocket으로 브로드캐스트할 WiFi 센서 신호 데이터 DTO
 * 프론트엔드의 실시간 그래프 업데이트 및 생존자 탐지 알림에 사용됨
 *
 * 브로드캐스트 토픽: /topic/wifi-sensor/{sensorId}/signal
 * 발행 빈도: 주기적 (생존자 탐지 여부와 무관)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WifiSignalDto {

    /**
     * WiFi 센서 ID (데이터베이스 Primary Key)
     * 예: 1, 2, 3
     */
    @JsonProperty("sensor_id")
    private Long sensorId;

    /**
     * 생존자 탐지 여부
     * true인 경우 프론트엔드에서 특수 효과 및 알림을 표시함
     */
    @JsonProperty("survivor_detected")
    private Boolean survivorDetected;

    /**
     * CSI 진폭 요약 데이터
     * ESP32에서 전송한 각 부반송파의 진폭값 배열
     * 프론트엔드 그래프 렌더링에 직접 사용됨
     */
    @JsonProperty("csi_amplitude_summary")
    private List<Double> csiAmplitudeSummary;

    /**
     * 메시지 수신 시각 (백엔드에서 추가)
     * 그래프의 X축 (시간축)에 사용됨
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * 생존자 ID (탐지된 경우에만)
     * 생존자가 탐지되고 DB에 저장된 경우 해당 Survivor의 ID를 포함함
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
     * MqttWifiDetectionDto로부터 WifiSignalDto를 생성하는 정적 팩토리 메서드
     * MQTT 메시지를 WebSocket 브로드캐스트용 DTO로 변환함
     *
     * @param mqttData MQTT로 수신한 원본 데이터
     * @param timestamp 백엔드에서 추가한 타임스탬프
     * @return WebSocket 브로드캐스트용 DTO
     */
    public static WifiSignalDto fromMqttData(MqttWifiDetectionDto mqttData, LocalDateTime timestamp) {
        return WifiSignalDto.builder()
                .sensorId(mqttData.getSensorId())
                .survivorDetected(mqttData.getSurvivorDetected())
                .csiAmplitudeSummary(mqttData.getCsiAmplitudeSummary())
                .timestamp(timestamp)
                .build();
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