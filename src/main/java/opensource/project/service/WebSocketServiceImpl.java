package opensource.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.DetectionResponseDto;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.SurvivorResponseDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 생존자의 우선순위 점수 업데이트를 구독자에게 브로드캐스트
     * 구독 주소: /topic/survivor/{survivorId}/scores
     */
    @Override
    public void broadcastPriorityScoreUpdate(Long survivorId, PriorityScoreHistoryDto scoreUpdate) {
        String destination = "/topic/survivor/" + survivorId + "/scores";
        messagingTemplate.convertAndSend(destination, scoreUpdate);
        log.info("Broadcasting priority score update to {}: {}", destination, scoreUpdate);
    }

    /**
     * 특정 생존자의 탐지 정보 업데이트를 구독자에게 브로드캐스트
     * 구독 주소: /topic/survivor/{survivorId}/detections
     */
    @Override
    public void broadcastDetectionUpdate(Long survivorId, DetectionResponseDto detection) {
        String destination = "/topic/survivor/" + survivorId + "/detections";
        messagingTemplate.convertAndSend(destination, detection);
        log.info("Broadcasting detection update to {}: {}", destination, detection);
    }

    /**
     * 특정 생존자 정보 업데이트를 구독자에게 브로드캐스트
     * 구독 주소: /topic/survivor/{survivorId}
     */
    @Override
    public void broadcastSurvivorUpdate(Long survivorId, SurvivorResponseDto survivor) {
        String destination = "/topic/survivor/" + survivorId;
        messagingTemplate.convertAndSend(destination, survivor);
        log.info("Broadcasting survivor update to {}: {}", destination, survivor);
    }

    /**
     * 새로운 생존자가 추가되었음을 구독자에게 브로드캐스트
     * 구독 주소: /topic/survivors/new
     */
    @Override
    public void broadcastNewSurvivorAdded(SurvivorResponseDto survivor) {
        String destination = "/topic/survivors/new";
        messagingTemplate.convertAndSend(destination, survivor);
        log.info("Broadcasting new survivor added to {}: survivor #{}", destination, survivor.getSurvivorNumber());
    }
}