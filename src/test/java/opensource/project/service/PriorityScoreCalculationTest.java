package opensource.project.service;

import opensource.project.domain.*;
import opensource.project.domain.enums.CCTVStatus;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;
import opensource.project.domain.enums.DetectionType;
import opensource.project.domain.enums.RescueStatus;
import opensource.project.dto.AIDetectionResultDto;
import opensource.project.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PriorityScoreCalculationTest {

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private SurvivorRepository survivorRepository;

    @Autowired
    private DetectionRepository detectionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private CCTVRepository cctvRepository;

    @Autowired
    private PriorityAssessmentRepository priorityAssessmentRepository;

    private Location testLocation;
    private CCTV testCctv;
    private Survivor testSurvivor;
    private Detection testDetection;

    @BeforeEach
    void setUp() {
        // 테스트용 위치 생성
        testLocation = Location.builder()
                .buildingName("테스트 건물")
                .floor(3)
                .roomNumber("301호")
                .build();
        testLocation = locationRepository.save(testLocation);

        // 테스트용 CCTV 생성
        testCctv = CCTV.builder()
                .cameraNumber(1)
                .cctvCode("TEST-CAM-01")
                .status(CCTVStatus.LIVE)
                .location(testLocation)
                .isActive(true)
                .build();
        testCctv = cctvRepository.save(testCctv);

        // 테스트용 생존자 생성
        testSurvivor = Survivor.builder()
                .survivorNumber(1)
                .location(testLocation)
                .currentStatus(CurrentStatus.FALLING)
                .detectionMethod(DetectionMethod.CCTV)
                .rescueStatus(RescueStatus.WAITING)
                .firstDetectedAt(LocalDateTime.now())
                .lastDetectedAt(LocalDateTime.now())
                .isActive(true)
                .isFalsePositive(false)
                .build();
        testSurvivor = survivorRepository.save(testSurvivor);

        // 테스트용 Detection 생성
        testDetection = Detection.builder()
                .survivor(testSurvivor)
                .detectionType(DetectionType.CCTV)
                .cctv(testCctv)
                .location(testLocation)
                .detectedAt(LocalDateTime.now())
                .detectedStatus(CurrentStatus.FALLING)
                .aiModelVersion("YOLO-ONNX-v1.0")
                .confidence(0.95)
                .build();
        testDetection = detectionRepository.save(testDetection);
    }

    @Test
    @DisplayName("Falling 자세 + smoke 30% 이상 → 위험도 점수 계산 테스트")
    void testRiskScoreCalculation_FallingPose_With_30PercentSmoke() {
        // given - AI 탐지 결과 생성 (Falling 자세 + smoke 30%)
        AIDetectionResultDto.DetectionObject humanDetection = createHumanDetection(
                "Falling",
                0.95,
                100, 100, 300, 500  // 사람 바운딩 박스
        );

        List<AIDetectionResultDto.DetectionObject> allDetections = new ArrayList<>();
        allDetections.add(humanDetection);

        // smoke 박스 3개 추가 (총 30% 면적)
        // 화면 크기: 1920 x 1080 기준
        // 30% = 화면크기 * 0.3 = 622,080
        // 각 smoke 박스: 약 207,360 픽셀 (720x288)
        allDetections.add(createSmokeDetection(0, 0, 720, 288));      // smoke 1
        allDetections.add(createSmokeDetection(720, 0, 1440, 288));   // smoke 2
        allDetections.add(createSmokeDetection(1440, 0, 1920, 216));  // smoke 3

        AIDetectionResultDto.DetectionSummary summary = new AIDetectionResultDto.DetectionSummary(
                0,  // fireCount
                1,  // humanCount
                3,  // smokeCount
                4   // totalObjects
        );

        // when - 위험도 점수 계산
        PriorityAssessment assessment = priorityService.createAssessmentFromAI(
                humanDetection,
                allDetections,
                summary,
                testSurvivor,
                testDetection
        );

        // then - 점수 검증
        assertThat(assessment).isNotNull();

        // 상태 점수: Falling = 10.0
        assertThat(assessment.getStatusScore()).isEqualTo(10.0);

        // 환경 점수: smoke 30%는 50% 미만이므로 checkDenseSmoke 통과 못함
        // fire가 없으므로 environmentMultiplier = 0.1 (화재 미감지)
        assertThat(assessment.getEnvironmentScore()).isEqualTo(0.1);

        // 최종 위험도 점수: 10.0 * 0.1 = 1.0
        assertThat(assessment.getFinalRiskScore()).isEqualTo(1.0);

        // 신뢰도 계수
        assertThat(assessment.getConfidenceCoefficient()).isEqualTo(0.95);

        // DB에 저장되었는지 확인
        assertThat(assessment.getId()).isNotNull();

        System.out.println("=== 테스트 결과 ===");
        System.out.println("Pose: Falling");
        System.out.println("Smoke 면적: 30%");
        System.out.println("상태 점수: " + assessment.getStatusScore());
        System.out.println("환경 점수: " + assessment.getEnvironmentScore());
        System.out.println("최종 위험도: " + assessment.getFinalRiskScore());
        System.out.println("긴급도: " + assessment.getUrgencyLevel());
    }

    @Test
    @DisplayName("Falling 자세 + smoke 50% 이상 → 고위험도 점수 계산 테스트")
    void testRiskScoreCalculation_FallingPose_With_50PercentSmoke() {
        // given - AI 탐지 결과 생성 (Falling 자세 + smoke 50% 이상)
        AIDetectionResultDto.DetectionObject humanDetection = createHumanDetection(
                "Falling",
                0.95,
                100, 100, 300, 500
        );

        List<AIDetectionResultDto.DetectionObject> allDetections = new ArrayList<>();
        allDetections.add(humanDetection);

        // smoke 박스 5개 추가 (총 50% 이상 면적)
        // 50% = 1,036,800 픽셀
        // 각 smoke 박스: 약 216,000 픽셀 (900x240)
        allDetections.add(createSmokeDetection(0, 0, 900, 240));
        allDetections.add(createSmokeDetection(900, 0, 1800, 240));
        allDetections.add(createSmokeDetection(0, 240, 900, 480));
        allDetections.add(createSmokeDetection(900, 240, 1800, 480));
        allDetections.add(createSmokeDetection(0, 480, 900, 720));

        AIDetectionResultDto.DetectionSummary summary = new AIDetectionResultDto.DetectionSummary(
                0,  // fireCount
                1,  // humanCount
                5,  // smokeCount
                6   // totalObjects
        );

        // when - 위험도 점수 계산
        PriorityAssessment assessment = priorityService.createAssessmentFromAI(
                humanDetection,
                allDetections,
                summary,
                testSurvivor,
                testDetection
        );

        // then - 점수 검증
        assertThat(assessment).isNotNull();

        // 상태 점수: Falling = 10.0
        assertThat(assessment.getStatusScore()).isEqualTo(10.0);

        // 환경 점수: smoke 50% 이상 → checkDenseSmoke = true → 2.0
        assertThat(assessment.getEnvironmentScore()).isEqualTo(2.0);

        // 최종 위험도 점수: 10.0 * 2.0 = 20.0
        assertThat(assessment.getFinalRiskScore()).isEqualTo(20.0);

        // 긴급도: 20.0 >= 8.0 → CRITICAL
        assertThat(assessment.getUrgencyLevel().name()).isEqualTo("CRITICAL");

        System.out.println("=== 테스트 결과 ===");
        System.out.println("Pose: Falling");
        System.out.println("Smoke 면적: 50% 이상");
        System.out.println("상태 점수: " + assessment.getStatusScore());
        System.out.println("환경 점수: " + assessment.getEnvironmentScore());
        System.out.println("최종 위험도: " + assessment.getFinalRiskScore());
        System.out.println("긴급도: " + assessment.getUrgencyLevel());
    }

    @Test
    @DisplayName("Falling 자세 + smoke 30% + fire 10% → 위험도 점수 계산 테스트")
    void testRiskScoreCalculation_FallingPose_With_30PercentSmoke_And_10PercentFire() {
        // given - AI 탐지 결과 생성 (Falling 자세 + smoke 30% + fire 10%)
        AIDetectionResultDto.DetectionObject humanDetection = createHumanDetection(
                "Falling",
                0.95,
                100, 100, 300, 500
        );

        List<AIDetectionResultDto.DetectionObject> allDetections = new ArrayList<>();
        allDetections.add(humanDetection);

        // smoke 박스 3개 (30%)
        allDetections.add(createSmokeDetection(0, 0, 720, 288));
        allDetections.add(createSmokeDetection(720, 0, 1440, 288));
        allDetections.add(createSmokeDetection(1440, 0, 1920, 216));

        // fire 박스 1개 (10% 면적)
        // 10% = 720x288
        allDetections.add(createFireDetection(500, 500, 1220, 788));

        AIDetectionResultDto.DetectionSummary summary = new AIDetectionResultDto.DetectionSummary(
                1,  // fireCount
                1,  // humanCount
                3,  // smokeCount
                5   // totalObjects
        );

        // when - 위험도 점수 계산
        PriorityAssessment assessment = priorityService.createAssessmentFromAI(
                humanDetection,
                allDetections,
                summary,
                testSurvivor,
                testDetection
        );

        // then - 점수 검증
        assertThat(assessment).isNotNull();

        // 상태 점수: Falling = 10.0
        assertThat(assessment.getStatusScore()).isEqualTo(10.0);

        // 환경 점수: fire 10% (5% 이상 30% 미만) → 단순 화재 감지 → 1.0
        assertThat(assessment.getEnvironmentScore()).isEqualTo(1.0);

        // 최종 위험도 점수: 10.0 * 1.0 = 10.0
        assertThat(assessment.getFinalRiskScore()).isEqualTo(10.0);

        // 긴급도: 10.0 >= 8.0 → CRITICAL
        assertThat(assessment.getUrgencyLevel().name()).isEqualTo("CRITICAL");

        System.out.println("=== 테스트 결과 ===");
        System.out.println("Pose: Falling");
        System.out.println("Smoke 면적: 30%, Fire 면적: 10%");
        System.out.println("상태 점수: " + assessment.getStatusScore());
        System.out.println("환경 점수: " + assessment.getEnvironmentScore());
        System.out.println("최종 위험도: " + assessment.getFinalRiskScore());
        System.out.println("긴급도: " + assessment.getUrgencyLevel());
    }

    @Test
    @DisplayName("다양한 자세별 위험도 점수 비교 테스트")
    void testRiskScoreCalculation_DifferentPoses() {
        // given - smoke 50% 환경에서 다양한 자세 테스트
        List<AIDetectionResultDto.DetectionObject> smokeDetections = new ArrayList<>();
        smokeDetections.add(createSmokeDetection(0, 0, 900, 240));
        smokeDetections.add(createSmokeDetection(900, 0, 1800, 240));
        smokeDetections.add(createSmokeDetection(0, 240, 900, 480));
        smokeDetections.add(createSmokeDetection(900, 240, 1800, 480));
        smokeDetections.add(createSmokeDetection(0, 480, 900, 720));

        AIDetectionResultDto.DetectionSummary summary = new AIDetectionResultDto.DetectionSummary(
                0, 1, 5, 6
        );

        // when & then - Falling (최고 위험)
        testPoseWithScore("Falling", smokeDetections, summary, 10.0, 20.0);

        // when & then - Crawling
        testPoseWithScore("Crawling", smokeDetections, summary, 8.0, 16.0);

        // when & then - Sitting
        testPoseWithScore("Sitting", smokeDetections, summary, 5.0, 10.0);

        // when & then - Standing (최저 위험)
        testPoseWithScore("Standing", smokeDetections, summary, 3.0, 6.0);
    }

    // ================ 헬퍼 메서드 ================

    private void testPoseWithScore(String pose,
                                     List<AIDetectionResultDto.DetectionObject> smokeDetections,
                                     AIDetectionResultDto.DetectionSummary summary,
                                     double expectedStatusScore,
                                     double expectedFinalScore) {
        AIDetectionResultDto.DetectionObject humanDetection = createHumanDetection(
                pose, 0.9, 100, 100, 300, 500
        );

        List<AIDetectionResultDto.DetectionObject> allDetections = new ArrayList<>();
        allDetections.add(humanDetection);
        allDetections.addAll(smokeDetections);

        PriorityAssessment assessment = priorityService.createAssessmentFromAI(
                humanDetection,
                allDetections,
                summary,
                testSurvivor,
                testDetection
        );

        assertThat(assessment.getStatusScore()).isEqualTo(expectedStatusScore);
        assertThat(assessment.getFinalRiskScore()).isEqualTo(expectedFinalScore);

        System.out.println(String.format("%s: 상태점수=%.1f, 최종위험도=%.1f, 긴급도=%s",
                pose, expectedStatusScore, expectedFinalScore, assessment.getUrgencyLevel()));
    }

    private AIDetectionResultDto.DetectionObject createHumanDetection(
            String pose, double confidence, int x1, int y1, int x2, int y2) {
        AIDetectionResultDto.DetectionObject detection = new AIDetectionResultDto.DetectionObject();
        detection.setClassName("human");
        detection.setConfidence(confidence);
        detection.setPose(pose);
        detection.setBox(new AIDetectionResultDto.BoundingBox(x1, y1, x2, y2));
        return detection;
    }

    private AIDetectionResultDto.DetectionObject createSmokeDetection(int x1, int y1, int x2, int y2) {
        AIDetectionResultDto.DetectionObject detection = new AIDetectionResultDto.DetectionObject();
        detection.setClassName("smoke");
        detection.setConfidence(0.9);
        detection.setBox(new AIDetectionResultDto.BoundingBox(x1, y1, x2, y2));
        return detection;
    }

    private AIDetectionResultDto.DetectionObject createFireDetection(int x1, int y1, int x2, int y2) {
        AIDetectionResultDto.DetectionObject detection = new AIDetectionResultDto.DetectionObject();
        detection.setClassName("fire");
        detection.setConfidence(0.9);
        detection.setBox(new AIDetectionResultDto.BoundingBox(x1, y1, x2, y2));
        return detection;
    }
}