package opensource.project.service;

import opensource.project.dto.DetectionResponseDto;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.dto.SurvivorResponseDto;
import opensource.project.dto.WifiSignalDto;

/**
 * WebSocket 브로드캐스트 서비스 인터페이스
 * 실시간 데이터를 프론트엔드에 전송하기 위한 메서드를 정의함
 */
public interface WebSocketService {

    /**
     * 특정 생존자의 우선순위 점수 업데이트를 구독자에게 브로드캐스트함
     *
     * @param survivorId 생존자 ID
     * @param scoreUpdate 우선순위 점수 이력 데이터
     */
    void broadcastPriorityScoreUpdate(Long survivorId, PriorityScoreHistoryDto scoreUpdate);

    /**
     * 특정 생존자의 탐지 정보 업데이트를 구독자에게 브로드캐스트함
     * CCTV 또는 WiFi 센서로 탐지된 정보를 전송함
     *
     * @param survivorId 생존자 ID
     * @param detection 탐지 정보 데이터
     */
    void broadcastDetectionUpdate(Long survivorId, DetectionResponseDto detection);

    /**
     * 특정 생존자 정보 업데이트를 구독자에게 브로드캐스트함
     *
     * @param survivorId 생존자 ID
     * @param survivor 생존자 정보 데이터
     */
    void broadcastSurvivorUpdate(Long survivorId, SurvivorResponseDto survivor);

    /**
     * 새로운 생존자가 추가되었음을 구독자에게 브로드캐스트함
     *
     * @param survivor 새로 추가된 생존자 정보
     */
    void broadcastNewSurvivorAdded(SurvivorResponseDto survivor);

    /**
     * [추가] WiFi 센서의 실시간 신호 데이터를 구독자에게 브로드캐스트함
     * 생존자 탐지 여부와 무관하게 5초마다 호출되며 프론트엔드의 실시간 그래프를 업데이트함
     *
     * 구독 토픽: /topic/wifi-sensor/{sensorId}/signal
     * 사용 시나리오:
     * - 평상시: 그래프만 업데이트 (survivorDetected = false)
     * - 생존자 탐지 시: 그래프 업데이트 + 특수 효과 트리거 (survivorDetected = true)
     *
     * @param sensorId WiFi 센서 ID (데이터베이스 ID, 예: 1, 2, 3)
     * @param signalData 신호 데이터 (CSI 분석 결과, 신호 강도, 생존자 탐지 여부 등 포함)
     */
    void broadcastWifiSignal(Long sensorId, WifiSignalDto signalData);

    /**
     * 최근 생존자 기록(타임아웃 스냅샷) 추가 브로드캐스트
     * 구독 토픽: /topic/recent-survivors
     */
    void broadcastRecentRecordAdded(opensource.project.dto.RecentSurvivorRecordResponseDto record);

    /**
     * 최근 생존자 기록 삭제 브로드캐스트
     * 구독 토픽: /topic/recent-survivors
     */
    void broadcastRecentRecordDeleted(Long recordId);
}
