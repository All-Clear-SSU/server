package opensource.project.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.dto.DetectionResponseDto;
import opensource.project.service.WebSocketService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/websocket/test")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketService webSocketService;

    // 커스텀 점수로 테스트 메시지 전송 - POST /websocket/test/survivor/{survivorId}/score?score=20.5
    @PostMapping("/survivor/{survivorId}/score")
    public ResponseEntity<String> sendCustomScoreMessage(
            @PathVariable Long survivorId,
            @RequestParam(defaultValue = "14.8") Double score) {

        String destination = "/topic/survivor/" + survivorId + "/scores";
        messagingTemplate.convertAndSend(destination, score);

        log.info("Test message sent to {}: {}", destination, score);

        return ResponseEntity.ok(
            String.format("Test message sent to %s\nScore: %.2f", destination, score)
        );
    }

    /**
     * 프론트엔드에서 /topic/survivor/{survivorId}/detections를 구독하고 있을 때
     * 이 엔드포인트를 호출하면 테스트 detection 메시지를 받을 수 있음
     */
    @PostMapping("/survivor/{survivorId}/detection")
    public ResponseEntity<String> sendTestDetectionMessage(@PathVariable Long survivorId) {
        log.info("=== TEST: Sending detection message for survivor {} ===", survivorId);

        // 테스트용 DetectionResponseDto 생성
        DetectionResponseDto testDetection = DetectionResponseDto.builder()
                .id(999L)
                .survivorId(survivorId)
                .detectionType(opensource.project.domain.enums.DetectionType.CCTV)
                .cctvId(1L)
                .locationId(1L)
                .detectedAt(LocalDateTime.now())
                .detectedStatus(opensource.project.domain.enums.CurrentStatus.STANDING)
                .aiAnalysisResult("TEST: Detection Test Message from WebSocket Test Controller")
                .aiModelVersion("TEST-v1.0")
                .confidence(0.95)
                .videoUrl("/home/videos/video.mp4")
                .build();

        // WebSocketService를 통해 브로드캐스트
        webSocketService.broadcastDetectionUpdate(survivorId, testDetection);

        String message = String.format(
            "[SUCCESS] Test detection message sent!\n" +
            "- Destination: /topic/survivor/%d/detections\n" +
            "- Detection ID: %d\n" +
            "- Detection Type: %s\n" +
            "- Status: %s\n" +
            "- Confidence: %.2f\n" +
            "- Message: %s\n\n" +
            "프론트엔드 콘솔에서 메시지를 확인해주세요!",
            survivorId,
            testDetection.getId(),
            testDetection.getDetectionType(),
            testDetection.getDetectedStatus(),
            testDetection.getConfidence(),
            testDetection.getAiAnalysisResult()
        );

        log.info("TEST: Detection message sent successfully to /topic/survivor/{}/detections", survivorId);

        return ResponseEntity.ok(message);
    }
}