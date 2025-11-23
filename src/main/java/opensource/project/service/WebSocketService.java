package opensource.project.service;

import opensource.project.dto.DetectionResponseDto;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.SurvivorResponseDto;

/**
 * WebSocket 브로드캐스트 서비스 인터페이스
 */
public interface WebSocketService {

    // 특정 생존자의 우선순위 점수 업데이트를 구독자에게 브로드캐스트
    void broadcastPriorityScoreUpdate(Long survivorId, PriorityScoreHistoryDto scoreUpdate);

    // 특정 생존자의 탐지 정보 업데이트를 구독자에게 브로드캐스트
    void broadcastDetectionUpdate(Long survivorId, DetectionResponseDto detection);

    // 특정 생존자 정보 업데이트를 구독자에게 브로드캐스트
    void broadcastSurvivorUpdate(Long survivorId, SurvivorResponseDto survivor);

    // 새로운 생존자가 추가되었음을 구독자에게 브로드캐스트
    void broadcastNewSurvivorAdded(SurvivorResponseDto survivor);
}