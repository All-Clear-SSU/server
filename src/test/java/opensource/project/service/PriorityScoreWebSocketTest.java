package opensource.project.service;

import opensource.project.domain.CCTV;
import opensource.project.domain.Detection;
import opensource.project.domain.Location;
import opensource.project.domain.Survivor;
import opensource.project.domain.enums.*;
import opensource.project.dto.AIDetectionResultDto;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.repository.CCTVRepository;
import opensource.project.repository.DetectionRepository;
import opensource.project.repository.LocationRepository;
import opensource.project.repository.SurvivorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class PriorityScoreWebSocketTest {

    @Autowired
    private PriorityService priorityService;

    @MockitoBean
    private WebSocketService webSocketService;

    @Autowired
    private SurvivorRepository survivorRepository;

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private CCTVRepository cctvRepository;

    private Survivor testSurvivor;
    private Detection testDetection;

    @BeforeEach
    void setUp() {
        Location location = locationRepository.save(
                Location.builder()
                        .buildingName("테스트 건물")
                        .floor(1)
                        .roomNumber("101호")
                        .build()
        );

        CCTV cctv = cctvRepository.save(
                CCTV.builder()
                        .cameraNumber(1)
                        .cctvCode("TEST-CAM-01")
                        .status(CCTVStatus.LIVE)
                        .location(location)
                        .isActive(true)
                        .build()
        );

        testSurvivor = survivorRepository.save(
                Survivor.builder()
                        .survivorNumber(1)
                        .location(location)
                        .currentStatus(CurrentStatus.FALLING)
                        .detectionMethod(DetectionMethod.CCTV)
                        .rescueStatus(RescueStatus.WAITING)
                        .firstDetectedAt(LocalDateTime.now())
                        .lastDetectedAt(LocalDateTime.now())
                        .isActive(true)
                        .isFalsePositive(false)
                        .build()
        );

        testDetection = detectionRepository.save(
                Detection.builder()
                        .survivor(testSurvivor)
                        .detectionType(DetectionType.CCTV)
                        .cctv(cctv)
                        .location(location)
                        .detectedAt(LocalDateTime.now())
                        .detectedStatus(CurrentStatus.FALLING)
                        .aiModelVersion("YOLO-ONNX-v1.0")
                        .confidence(0.95)
                        .build()
        );
    }

    @Test
    @DisplayName("위험도 점수 계산 시 WebSocket 브로드캐스트 검증")
    void testWebSocketBroadcast_WhenCreateAssessment() {
        // given
        AIDetectionResultDto.DetectionObject humanDetection = new AIDetectionResultDto.DetectionObject();
        humanDetection.setClassName("human");
        humanDetection.setConfidence(0.95);
        humanDetection.setPose("Falling");
        humanDetection.setBox(new AIDetectionResultDto.BoundingBox(100, 100, 300, 500));

        List<AIDetectionResultDto.DetectionObject> allDetections = new ArrayList<>();
        allDetections.add(humanDetection);

        AIDetectionResultDto.DetectionSummary summary = new AIDetectionResultDto.DetectionSummary(
                0, 1, 0, 1
        );

        // when
        priorityService.createAssessmentFromAI(
                humanDetection,
                allDetections,
                summary,
                testSurvivor,
                testDetection
        );

        // then - WebSocket 브로드캐스트가 1번 호출되었는지 검증
        verify(webSocketService, times(1))
                .broadcastPriorityScoreUpdate(
                        eq(testSurvivor.getId()),
                        any(PriorityScoreHistoryDto.class)
                );
    }
}