package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * MQTT 브로커로부터 수신한 WiFi 센서 탐지 메시지를 파싱하는 DTO
 * ESP32 모듈이 주기적으로 발행하는 CSI 신호 분석 결과를 담음
 *
 * 메시지 흐름:
 * ESP32 → MQTT 브로커 → Spring Boot (이 DTO로 파싱) → 비즈니스 로직 처리
 *
 * ESP32에서 전송하는 데이터:
 * - sensor_id: WiFi 센서 ID
 * - survivor_detected: 생존자 탐지 여부
 * - csi_amplitude_summary: CSI 진폭 배열
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MqttWifiDetectionDto {

    /**
     * WiFi 센서 ID (데이터베이스 Primary Key)
     * WifiSensor 엔티티의 id와 직접 매칭됨
     * 예: 1, 2, 3
     */
    @JsonProperty("sensor_id")
    private Long sensorId;

    /**
     * 생존자 탐지 여부
     * AI 모델이 CSI 신호를 분석하여 생존자 존재를 판단한 결과
     * true: 생존자 탐지됨 → DB 저장 및 특수 효과 트리거
     * false: 생존자 없음 → WebSocket 브로드캐스트만 수행 (그래프 업데이트용)
     */
    @JsonProperty("survivor_detected")
    private Boolean survivorDetected;

    /**
     * CSI 진폭 요약 데이터
     * ESP32에서 계산한 각 부반송파의 진폭값 배열 (일반적으로 34개 또는 52개)
     * 프론트엔드 그래프 렌더링에 직접 사용됨
     * 예: [12.3, 15.7, 18.2, 21.5, ..., 22.1]
     */
    @JsonProperty("csi_amplitude_summary")
    private List<Double> csiAmplitudeSummary;
}