package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.DetectionResponseDto;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.SurvivorResponseDto;
import opensource.project.dto.WifiSignalDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 브로드캐스트 서비스 구현체
 * SimpMessagingTemplate을 사용하여 STOMP 프로토콜로 메시지를 전송함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 생존자의 우선순위 점수 업데이트를 구독자에게 브로드캐스트함
     * 구독 토픽: /topic/survivor/{survivorId}/scores
     */
    @Override
    public void broadcastPriorityScoreUpdate(Long survivorId, PriorityScoreHistoryDto scoreUpdate) {
        String destination = "/topic/survivor/" + survivorId + "/scores";
        messagingTemplate.convertAndSend(destination, scoreUpdate);
        log.info("Broadcasting priority score update to {}: {}", destination, scoreUpdate);
    }

    /**
     * 특정 생존자의 탐지 정보 업데이트를 구독자에게 브로드캐스트함
     * CCTV 또는 WiFi 센서로 탐지된 정보를 전송함
     * 구독 토픽: /topic/survivor/{survivorId}/detections
     */
    @Override
    public void broadcastDetectionUpdate(Long survivorId, DetectionResponseDto detection) {
        String destination = "/topic/survivor/" + survivorId + "/detections";
        messagingTemplate.convertAndSend(destination, detection);
        log.info("Broadcasting detection update to {}: {}", destination, detection);
    }

    /**
     * 특정 생존자 정보 업데이트를 구독자에게 브로드캐스트함
     * 구독 토픽: /topic/survivor/{survivorId}
     */
    @Override
    public void broadcastSurvivorUpdate(Long survivorId, SurvivorResponseDto survivor) {
        String destination = "/topic/survivor/" + survivorId;
        messagingTemplate.convertAndSend(destination, survivor);
        log.info("Broadcasting survivor update to {}: {}", destination, survivor);
    }

    /**
     * 새로운 생존자가 추가되었음을 구독자에게 브로드캐스트함
     * 구독 토픽: /topic/survivors/new
     */
    @Override
    public void broadcastNewSurvivorAdded(SurvivorResponseDto survivor) {
        String destination = "/topic/survivors/new";
        messagingTemplate.convertAndSend(destination, survivor);
        log.info("Broadcasting new survivor added to {}: survivor #{}", destination, survivor.getSurvivorNumber());
    }

    /**
     * [추가] WiFi 센서의 실시간 신호 데이터를 구독자에게 브로드캐스트함
     * 5초마다 호출되며 생존자 탐지 여부와 무관하게 항상 실행됨
     * 프론트엔드의 실시간 그래프를 업데이트하고, 생존자가 탐지된 경우 특수 효과를 트리거함
     *
     * 구독 토픽: /topic/wifi-sensor/{sensorId}/signal
     *
     * @param sensorId WiFi 센서 ID (예: "ESP32-001")
     * @param signalData 신호 데이터 (CSI 분석 결과, 신호 강도, 생존자 탐지 여부 등)
     */
    @Override
    public void broadcastWifiSignal(String sensorId, WifiSignalDto signalData) {
        String destination = "/topic/wifi-sensor/" + sensorId + "/signal";
        messagingTemplate.convertAndSend(destination, signalData);

        // 생존자가 탐지된 경우에만 상세 로그를 남김 (평상시에는 로그 스팸 방지)
        if (Boolean.TRUE.equals(signalData.getSurvivorDetected())) {
            log.info("⚠️ [생존자 탐지!] WiFi 신호 브로드캐스트 - 토픽: {}, 센서: {}, 생존자 ID: {}, 신호 강도: {} dBm, 신뢰도: {}",
                    destination,
                    sensorId,
                    signalData.getSurvivorId(),
                    signalData.getSignalStrength(),
                    signalData.getConfidence());
        } else {
            // 평상시에는 DEBUG 레벨로 로그를 남김 (운영 환경에서는 출력되지 않음)
            log.debug("WiFi 신호 브로드캐스트 - 토픽: {}, 센서: {}, 신호 강도: {} dBm, 생존자 탐지: {}",
                    destination,
                    sensorId,
                    signalData.getSignalStrength(),
                    signalData.getSurvivorDetected());
        }
    }
}