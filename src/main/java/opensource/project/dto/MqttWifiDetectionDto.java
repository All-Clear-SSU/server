package opensource.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * MQTT 브로커로부터 수신한 WiFi 센서 탐지 메시지를 파싱하는 DTO
 * ESP32 모듈이 5초마다 발행하는 CSI 신호 분석 결과를 담음
 *
 * 메시지 흐름:
 * ESP32 → MQTT 브로커 → Spring Boot (이 DTO로 파싱) → 비즈니스 로직 처리
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MqttWifiDetectionDto {

    /**
     * WiFi 센서 ID (고유 식별자)
     * ESP32 모듈의 MAC 주소 또는 사용자 정의 ID를 사용함
     * 예: "ESP32-001", "ESP32-AABBCCDDEE"
     * WifiSensor 엔티티의 sensorCode와 매칭됨
     */
    @JsonProperty("sensor_id")
    private String sensorId;

    /**
     * 위치 ID
     * 센서가 설치된 위치를 나타냄
     * Location 엔티티의 id와 매칭됨
     */
    @JsonProperty("location_id")
    private Long locationId;

    /**
     * 생존자 탐지 여부
     * AI 모델이 CSI 신호를 분석하여 생존자 존재를 판단한 결과
     * true: 생존자 탐지됨 → DB 저장 및 특수 효과 트리거
     * false: 생존자 없음 → WebSocket 브로드캐스트만 수행 (그래프 업데이트용)
     */
    @JsonProperty("survivor_detected")
    private Boolean survivorDetected;

    /**
     * 전체 신호 강도 (RSSI, dBm 단위)
     * 수신 신호 강도 지표로, 센서와 생존자 간 대략적인 거리를 추정하는 데 사용됨
     * 일반적인 범위: -30 dBm (매우 가까움) ~ -90 dBm (매우 멀거나 약함)
     */
    @JsonProperty("signal_strength")
    private Integer signalStrength;

    /**
     * AI 모델의 탐지 신뢰도 (0.0 ~ 1.0)
     * 생존자가 실제로 존재할 확률을 나타냄
     * 0.5 미만: 낮은 신뢰도, 0.5~0.8: 중간 신뢰도, 0.8 이상: 높은 신뢰도
     */
    @JsonProperty("confidence")
    private Double confidence;

    /**
     * 메시지 생성 시각
     * ESP32 모듈이 CSI 데이터를 수집하고 메시지를 생성한 시각
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * CSI 신호 분석 데이터
     * ESP32에서 수집한 CSI 원시 데이터 및 AI 분석 결과를 포함함
     * 그래프 렌더링에 필요한 모든 정보가 담겨 있음
     */
    @JsonProperty("csi_analysis")
    private WifiAnalysisDataDto csiAnalysis;

    /**
     * 센서 배터리 잔량 (퍼센트, 0~100)
     * 배터리로 작동하는 ESP32 모듈의 경우 잔량을 전송함
     * null인 경우 유선 전원 사용 중
     */
    @JsonProperty("battery_level")
    private Integer batteryLevel;

    /**
     * 센서 상태 메시지
     * 센서의 현재 상태나 오류 정보를 담음
     * 예: "OK", "LOW_BATTERY", "CALIBRATING", "ERROR"
     */
    @JsonProperty("status_message")
    private String statusMessage;
}