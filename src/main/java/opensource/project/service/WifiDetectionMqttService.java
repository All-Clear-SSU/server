package opensource.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.Location;
import opensource.project.domain.WifiSensor;
import opensource.project.dto.MqttWifiDetectionDto;
import opensource.project.dto.WifiSignalDto;
import opensource.project.repository.LocationRepository;
import opensource.project.repository.WifiSensorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MQTT로부터 수신한 WiFi 센서 데이터를 처리하는 서비스
 *
 * 주요 역할:
 * 1. MQTT 메시지를 수신하여 검증함
 * 2. WiFi 센서 정보를 조회함
 * 3. 항상 WebSocket으로 실시간 신호 데이터를 브로드캐스트함 (그래프 업데이트용)
 * 4. 생존자가 탐지된 경우에만 WifiDetectionProcessorService를 호출하여 DB 저장 및 생존자 매칭을 수행함
 *
 * 처리 흐름:
 * MQTT 메시지 수신 → 센서 조회 → WebSocket 브로드캐스트 → (생존자 탐지 시) DB 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WifiDetectionMqttService {

    private final WifiSensorRepository wifiSensorRepository;
    private final LocationRepository locationRepository;
    private final WebSocketService webSocketService;
    private final WifiDetectionProcessorService wifiDetectionProcessorService;
    private final ObjectMapper objectMapper;

    /**
     * MQTT 메시지를 처리하는 메인 메서드
     * 주기적으로 호출되며 생존자 탐지 여부와 무관하게 항상 실행됨
     *
     * @param mqttData MQTT 브로커로부터 수신한 WiFi 센서 데이터
     */
    @Transactional
    public void processMqttMessage(MqttWifiDetectionDto mqttData) {
        log.debug("WiFi 탐지 메시지 처리 시작 - 센서: {}", mqttData.getSensorId());

        try {
            // 1. 입력 데이터 유효성 검증을 수행함
            validateMqttData(mqttData);

            // 2. WiFi 센서 정보를 ID로 조회함
            WifiSensor sensor = wifiSensorRepository.findById(mqttData.getSensorId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "WiFi 센서를 찾을 수 없습니다. 센서 ID: " + mqttData.getSensorId()));

            // 3. 센서에 연결된 위치 정보를 조회함
            Location location = sensor.getLocation();
            if (location == null) {
                throw new IllegalArgumentException("센서에 연결된 위치 정보가 없습니다. 센서 ID: " + mqttData.getSensorId());
            }

            // 4. 타임스탬프를 백엔드에서 생성함 (MQTT 메시지에는 포함되지 않음)
            java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();

            // 5. 센서의 마지막 활성 시각을 업데이트함
            sensor.setLastActiveAt(timestamp);
            sensor.setIsActive(true);
            wifiSensorRepository.save(sensor);

            // 6. WebSocket 브로드캐스트용 DTO를 생성함
            WifiSignalDto signalDto = WifiSignalDto.fromMqttData(mqttData, timestamp);

            // 7. [항상 수행] WebSocket으로 실시간 신호 데이터를 브로드캐스트함
            // 프론트엔드의 그래프가 주기적으로 업데이트됨
            webSocketService.broadcastWifiSignal(mqttData.getSensorId(), signalDto);
            log.debug("WebSocket 브로드캐스트 완료 - 토픽: /topic/wifi-sensor/{}/signal", mqttData.getSensorId());

            // 8. ✅ 생존자 탐지 여부와 무관하게 항상 처리를 수행함 (false 신호도 웹에 표시)
            if (Boolean.TRUE.equals(mqttData.getSurvivorDetected())) {
                log.info("⚠️ 생존자 탐지됨! 센서: {}, 위치: {}",
                        mqttData.getSensorId(), location.getFullAddress());
            } else {
                log.debug("생존자 미탐지 - 센서: {}, 위치: {}",
                        mqttData.getSensorId(), location.getFullAddress());
            }

            // 생존자 매칭 및 Detection 레코드 DB 저장을 수행함 (탐지 여부와 무관)
            wifiDetectionProcessorService.processDetection(mqttData, sensor, location, signalDto, timestamp);

            log.info("WiFi 센서 처리 완료 - 센서: {}, 생존자 탐지: {}",
                    mqttData.getSensorId(), mqttData.getSurvivorDetected());

        } catch (IllegalArgumentException e) {
            // 센서 또는 위치를 찾을 수 없는 경우의 예외 처리
            log.error("WiFi 탐지 메시지 처리 실패 - 유효성 검증 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // 기타 예외 발생 시 에러 로그를 남김
            log.error("WiFi 탐지 메시지 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("WiFi 탐지 메시지 처리 실패", e);
        }
    }

    /**
     * MQTT 데이터의 유효성을 검증함
     * 필수 필드가 null이거나 유효하지 않은 경우 예외를 발생시킴
     *
     * @param mqttData 검증할 MQTT 데이터
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    private void validateMqttData(MqttWifiDetectionDto mqttData) {
        if (mqttData == null) {
            throw new IllegalArgumentException("MQTT 데이터가 null입니다.");
        }

        if (mqttData.getSensorId() == null) {
            throw new IllegalArgumentException("센서 ID가 null입니다.");
        }

        if (mqttData.getSurvivorDetected() == null) {
            throw new IllegalArgumentException("생존자 탐지 여부가 null입니다.");
        }

        if (mqttData.getCsiAmplitudeSummary() == null || mqttData.getCsiAmplitudeSummary().isEmpty()) {
            throw new IllegalArgumentException("CSI 진폭 데이터가 null이거나 비어있습니다.");
        }

        log.debug("MQTT 데이터 유효성 검증 완료");
    }
}