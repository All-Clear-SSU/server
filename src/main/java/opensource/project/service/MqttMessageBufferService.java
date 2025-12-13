package opensource.project.service;

import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.MqttWifiDetectionDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT 메시지를 버퍼링하여 센서별로 최신 메시지만 유지하는 서비스
 *
 * 문제: MQTT 메시지가 너무 많이 들어와서 처리가 밀리는 현상 발생
 * 해결: 센서별로 최신 메시지만 버퍼에 유지하고, 주기적으로 배치 처리
 *
 * 동작 방식:
 * 1. MQTT 메시지가 들어오면 센서 ID를 키로 하여 최신 메시지만 맵에 저장 (이전 메시지는 자동 폐기)
 * 2. 100ms마다 버퍼에 있는 최신 메시지들을 배치로 처리
 * 3. 처리된 메시지는 버퍼에서 제거
 *
 * 장점:
 * - 센서별로 최신 상태만 반영되므로 밀린 메시지는 자동으로 무시됨
 * - 웹에는 항상 최신 데이터가 표시됨
 * - 메모리 사용량이 센서 개수에만 비례 (센서 수 × 메시지 크기)
 */
@Slf4j
@Service
public class MqttMessageBufferService {

    private final WifiDetectionMqttService wifiDetectionMqttService;

    // 센서별 최신 메시지를 저장하는 맵 (센서 ID → 최신 메시지)
    private final ConcurrentHashMap<Long, MqttWifiDetectionDto> latestMessagesBySensor = new ConcurrentHashMap<>();

    // 통계: 폐기된 메시지 개수
    private final AtomicInteger discardedMessageCount = new AtomicInteger(0);

    public MqttMessageBufferService(WifiDetectionMqttService wifiDetectionMqttService) {
        this.wifiDetectionMqttService = wifiDetectionMqttService;
    }

    /**
     * MQTT 메시지를 버퍼에 추가 (비동기)
     * 같은 센서의 이전 메시지는 자동으로 폐기됨
     *
     * @param mqttData MQTT 메시지
     */
    @Async
    public void bufferMessage(MqttWifiDetectionDto mqttData) {
        if (mqttData == null || mqttData.getSensorId() == null) {
            log.warn("유효하지 않은 MQTT 메시지 무시: {}", mqttData);
            return;
        }

        Long sensorId = mqttData.getSensorId();

        // 이전 메시지가 있으면 폐기됨
        MqttWifiDetectionDto previousMessage = latestMessagesBySensor.put(sensorId, mqttData);

        if (previousMessage != null) {
            discardedMessageCount.incrementAndGet();
            log.debug("센서 {}의 이전 메시지 폐기 (최신 메시지로 교체)", sensorId);
        }
    }

    /**
     * 버퍼에 있는 최신 메시지들을 주기적으로 처리
     * 100ms마다 실행됨
     */
    @Scheduled(fixedDelay = 100)
    public void processBufferedMessages() {
        if (latestMessagesBySensor.isEmpty()) {
            return;
        }

        // 버퍼에서 모든 메시지를 꺼냄
        ConcurrentHashMap<Long, MqttWifiDetectionDto> messagesToProcess = new ConcurrentHashMap<>(latestMessagesBySensor);
        latestMessagesBySensor.clear();

        log.debug("버퍼 처리 시작 - {} 개 센서의 최신 메시지 처리", messagesToProcess.size());

        // 각 센서의 최신 메시지를 처리
        messagesToProcess.values().forEach(mqttData -> {
            try {
                wifiDetectionMqttService.processMqttMessage(mqttData);
            } catch (Exception e) {
                log.error("MQTT 메시지 처리 실패 - 센서 ID: {}, 오류: {}",
                        mqttData.getSensorId(), e.getMessage(), e);
            }
        });

        log.debug("버퍼 처리 완료 - {} 개 메시지 처리됨", messagesToProcess.size());
    }

    /**
     * 통계 정보를 주기적으로 출력 (10초마다)
     */
    @Scheduled(fixedDelay = 10000)
    public void logStatistics() {
        int discarded = discardedMessageCount.getAndSet(0);
        if (discarded > 0) {
            log.info("📊 MQTT 메시지 통계 - 최근 10초간 폐기된 메시지: {} 개 (최신 메시지만 유지됨)", discarded);
        }
    }

    /**
     * 버퍼 상태 조회 (디버깅용)
     */
    public int getBufferSize() {
        return latestMessagesBySensor.size();
    }
}