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
     * 5초마다 호출되며 생존자 탐지 여부와 무관하게 항상 실행됨
     *
     * @param mqttData MQTT 브로커로부터 수신한 WiFi 센서 데이터
     */
    @Transactional
    public void processMqttMessage(MqttWifiDetectionDto mqttData) {
        log.debug("WiFi 탐지 메시지 처리 시작 - 센서: {}, 위치: {}",
                mqttData.getSensorId(), mqttData.getLocationId());

        try {
            // 1. 입력 데이터 유효성 검증을 수행함
            validateMqttData(mqttData);

            // 2. WiFi 센서 정보를 조회함
            WifiSensor sensor = wifiSensorRepository.findBySensorCode(mqttData.getSensorId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "WiFi 센서를 찾을 수 없습니다. 센서 ID: " + mqttData.getSensorId()));

            // 3. 위치 정보를 조회함
            Location location = locationRepository.findById(mqttData.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "위치 정보를 찾을 수 없습니다. 위치 ID: " + mqttData.getLocationId()));

            // 4. 센서의 마지막 활성 시각을 업데이트함
            sensor.setLastActiveAt(mqttData.getTimestamp());
            sensor.setIsActive(true);
            sensor.setSignalStrength(mqttData.getSignalStrength());
            wifiSensorRepository.save(sensor);

            // 5. WebSocket 브로드캐스트용 DTO를 생성함
            WifiSignalDto signalDto = WifiSignalDto.fromMqttData(
                    mqttData,
                    sensor.getSensorCode(),
                    location.getFullAddress()
            );

            // 6. [항상 수행] WebSocket으로 실시간 신호 데이터를 브로드캐스트함
            // 프론트엔드의 그래프가 5초마다 업데이트됨
            webSocketService.broadcastWifiSignal(mqttData.getSensorId(), signalDto);
            log.debug("WebSocket 브로드캐스트 완료 - 토픽: /topic/wifi-sensor/{}/signal", mqttData.getSensorId());

            // 7. 생존자가 탐지된 경우에만 추가 처리를 수행함
            if (Boolean.TRUE.equals(mqttData.getSurvivorDetected())) {
                log.info("⚠️ 생존자 탐지됨! 센서: {}, 신뢰도: {}, 신호 강도: {} dBm",
                        mqttData.getSensorId(),
                        mqttData.getConfidence(),
                        mqttData.getSignalStrength());

                // 생존자 매칭 및 Detection 레코드 DB 저장을 수행함
                wifiDetectionProcessorService.processDetection(mqttData, sensor, location, signalDto);

                log.info("생존자 탐지 처리 완료 - 센서: {}", mqttData.getSensorId());
            } else {
                log.debug("생존자 미탐지 - 센서: {}, WebSocket 브로드캐스트만 수행", mqttData.getSensorId());
            }

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

        if (mqttData.getSensorId() == null || mqttData.getSensorId().trim().isEmpty()) {
            throw new IllegalArgumentException("센서 ID가 null이거나 비어있습니다.");
        }

        if (mqttData.getLocationId() == null) {
            throw new IllegalArgumentException("위치 ID가 null입니다.");
        }

        if (mqttData.getSurvivorDetected() == null) {
            throw new IllegalArgumentException("생존자 탐지 여부가 null입니다.");
        }

        if (mqttData.getTimestamp() == null) {
            throw new IllegalArgumentException("타임스탬프가 null입니다.");
        }

        // 생존자가 탐지된 경우 추가 필드 검증을 수행함
        if (Boolean.TRUE.equals(mqttData.getSurvivorDetected())) {
            if (mqttData.getConfidence() == null || mqttData.getConfidence() < 0.0 || mqttData.getConfidence() > 1.0) {
                throw new IllegalArgumentException("신뢰도 값이 유효하지 않습니다. 0.0 ~ 1.0 범위여야 합니다.");
            }

            if (mqttData.getCsiAnalysis() == null) {
                log.warn("생존자가 탐지되었으나 CSI 분석 데이터가 없습니다. 센서: {}", mqttData.getSensorId());
            }
        }

        log.debug("MQTT 데이터 유효성 검증 완료");
    }

    /**
     * CSI 분석 데이터를 JSON 문자열로 직렬화함
     * Detection 엔티티의 rawData 필드에 저장하기 위해 사용됨
     *
     * @param mqttData MQTT 데이터 (CSI 분석 데이터 포함)
     * @return JSON 문자열 (실패 시 빈 객체 "{}")
     */
    public String serializeCsiAnalysisToJson(MqttWifiDetectionDto mqttData) {
        try {
            if (mqttData.getCsiAnalysis() != null) {
                return objectMapper.writeValueAsString(mqttData.getCsiAnalysis());
            }
            return "{}";
        } catch (JsonProcessingException e) {
            log.error("CSI 분석 데이터 JSON 직렬화 실패: {}", e.getMessage(), e);
            return "{}";
        }
    }
}