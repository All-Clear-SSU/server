package opensource.project.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opensource.project.domain.Detection;
import opensource.project.domain.PriorityAssessment;
import opensource.project.domain.Survivor;
import opensource.project.dto.AIDetectionResultDto;
import opensource.project.dto.PriorityAssessmentRequestDto;
import opensource.project.dto.PriorityAssessmentResponseDto;
import opensource.project.dto.PriorityScoreHistoryDto;
import opensource.project.repository.DetectionRepository;
import opensource.project.repository.PriorityAssessmentRepository;
import opensource.project.repository.SurvivorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriorityServiceImpl implements PriorityService {

    private final PriorityAssessmentRepository priorityAssessmentRepository;
    private final SurvivorRepository survivorRepository;
    private final DetectionRepository detectionRepository;
    private final WebSocketService webSocketService;

    // 생존자의 분석 점수 생성
    @Override
    @Transactional
    public PriorityAssessmentResponseDto createPriorityAssessment(PriorityAssessmentRequestDto requestDto) {
        Survivor survivor = survivorRepository.findById(requestDto.getSurvivorId())
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + requestDto.getSurvivorId()));

        Detection detection = detectionRepository.findById(requestDto.getDetectionId())
                .orElseThrow(() -> new IllegalArgumentException("Detection not found with id: " + requestDto.getDetectionId()));

        PriorityAssessment assessment = PriorityAssessment.builder()
                .survivor(survivor)
                .detection(detection)
                .assessedAt(requestDto.getAssessedAt())
                .statusScore(requestDto.getStatusScore())
                .environmentScore(requestDto.getEnvironmentScore())
                .confidenceCoefficient(requestDto.getConfidenceCoefficient())
                .finalRiskScore(requestDto.getFinalRiskScore())
                .calculationFormula(requestDto.getCalculationFormula())
                .aiModelVersion(requestDto.getAiModelVersion())
                .notes(requestDto.getNotes())
                .build();

        PriorityAssessment savedAssessment = priorityAssessmentRepository.save(assessment);

        // WebSocket으로 실시간 브로드캐스트
        PriorityScoreHistoryDto scoreDto = PriorityScoreHistoryDto.from(savedAssessment);
        webSocketService.broadcastPriorityScoreUpdate(requestDto.getSurvivorId(), scoreDto);

        return PriorityAssessmentResponseDto.fromWithoutRelations(savedAssessment);
    }

    // 모든 생존자의 분석 점수 반환
    @Override
    public List<PriorityAssessmentResponseDto> getAllPriorityAssessments() {
        return priorityAssessmentRepository.findAll().stream()
                .map(PriorityAssessmentResponseDto::from)
                .collect(Collectors.toList());
    }

    // 특정 생존자의 분석 점수 반환
    @Override
    public PriorityAssessmentResponseDto getPriorityAssessment(Long id) {
        PriorityAssessment assessment = priorityAssessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PriorityAssessment not found with id: " + id));
        return PriorityAssessmentResponseDto.from(assessment);
    }

    // 분석 점수 수정
    @Override
    @Transactional
    public PriorityAssessmentResponseDto updatePriorityAssessment(Long id, PriorityAssessmentRequestDto requestDto) {
        PriorityAssessment assessment = priorityAssessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PriorityAssessment not found with id: " + id));

        Survivor survivor = survivorRepository.findById(requestDto.getSurvivorId())
                .orElseThrow(() -> new IllegalArgumentException("Survivor not found with id: " + requestDto.getSurvivorId()));

        Detection detection = detectionRepository.findById(requestDto.getDetectionId())
                .orElseThrow(() -> new IllegalArgumentException("Detection not found with id: " + requestDto.getDetectionId()));

        assessment.setSurvivor(survivor);
        assessment.setDetection(detection);
        assessment.setAssessedAt(requestDto.getAssessedAt());
        assessment.setStatusScore(requestDto.getStatusScore());
        assessment.setEnvironmentScore(requestDto.getEnvironmentScore());
        assessment.setConfidenceCoefficient(requestDto.getConfidenceCoefficient());
        assessment.setFinalRiskScore(requestDto.getFinalRiskScore());
        assessment.setCalculationFormula(requestDto.getCalculationFormula());
        assessment.setAiModelVersion(requestDto.getAiModelVersion());
        assessment.setNotes(requestDto.getNotes());

        // WebSocket으로 실시간 브로드캐스트
        PriorityScoreHistoryDto scoreDto = PriorityScoreHistoryDto.from(assessment);
        webSocketService.broadcastPriorityScoreUpdate(requestDto.getSurvivorId(), scoreDto);

        return PriorityAssessmentResponseDto.from(assessment);
    }

    // 특정 생존자의 분석 점수 삭제
    @Override
    @Transactional
    public void deletePriorityAssessment(Long id) {
        if (!priorityAssessmentRepository.existsById(id)) {
            throw new IllegalArgumentException("PriorityAssessment not found with id: " + id);
        }
        priorityAssessmentRepository.deleteById(id);
    }

    // 특정 생존자의 가장 최근 분석 점수 가져오기
    @Override
    public PriorityAssessmentResponseDto getLatestAssessmentForSurvivor(Long survivorId) {
        PriorityAssessment assessment = priorityAssessmentRepository.findFirstBySurvivor_IdOrderByAssessedAtDesc(survivorId)
                .orElseThrow(() -> new IllegalArgumentException("No assessment found for survivor with id: " + survivorId));
        return PriorityAssessmentResponseDto.from(assessment);
    }

    // 더미 PriorityAssessment 데이터를 생성, Posing 모델이 개발되기 전까지 임시로 사용
    @Override
    public PriorityAssessmentRequestDto
        createDummyPriorityAssessment
    (Long detectionId, Long survivorId) {
        // 더미 데이터 생성
        double statusScore = 7; // 상태점수 (임시값)
        double environmentScore = 2;    // 신뢰도계수 (임시값)
        double confidenceCoefficient = 0.85;    // 신뢰도계수 (임시값)
        double finalRiskScore = statusScore * environmentScore; // 최종위험점수 계산

        return PriorityAssessmentRequestDto.builder()
                .survivorId(survivorId)
                .detectionId(detectionId)
                .assessedAt(java.time.LocalDateTime.now())
                .statusScore(statusScore)
                .environmentScore(environmentScore)
                .confidenceCoefficient(confidenceCoefficient)
                .finalRiskScore(finalRiskScore)
                .calculationFormula("(statusScore + environmentScore) * confidenceCoefficient / 2")
                .aiModelVersion("Dummy-v1.0")
                .notes("Posing 모델 개발 전 더미 데이터")
                .build();
    }

    // ==================== WeightScore 기준 위험도 점수 계산 ====================

    // AI 모델 분석 결과 기반으로 PriorityAssessment 생성 및 저장
    @Override
    @Transactional
    public PriorityAssessment createAssessmentFromAI(
            AIDetectionResultDto.DetectionObject humanDetection,
            List<AIDetectionResultDto.DetectionObject> allDetections,
            AIDetectionResultDto.DetectionSummary summary,
            Survivor survivor,
            Detection detection) {

        LocalDateTime now = LocalDateTime.now();

        // 위험도 점수 계산
        ScoreResult scoreResult = calculateRiskScore(humanDetection, allDetections, summary);
        double statusScore = scoreResult.getStatusScore();
        double environmentScore = scoreResult.getEnvironmentMultiplier();
        double confidenceCoefficient = humanDetection.getConfidence() != null
                ? humanDetection.getConfidence() : 1.0;
        // 최종 위험도점수 = 상태점수 * 환경점수(승수)
        double finalRiskScore = statusScore * environmentScore;

        // 계산 공식
        String formula = String.format(
                "상태점수(%.1f) × 환경승수(%.1f) = %.2f (신뢰도: %.2f)",
                statusScore, environmentScore, finalRiskScore, confidenceCoefficient);

        PriorityAssessment assessment = PriorityAssessment.builder()
                .survivor(survivor)
                .detection(detection)
                .assessedAt(now)
                .statusScore(statusScore)
                .environmentScore(environmentScore)
                .confidenceCoefficient(confidenceCoefficient)
                .finalRiskScore(finalRiskScore)
                .calculationFormula(formula)
                .aiModelVersion("YOLO-ONNX-v1.0")
                .notes(String.format("Pose: %s, Fire: %d, Smoke: %d",
                        humanDetection.getPose(),
                        summary.getFireCount(),
                        summary.getSmokeCount()))
                .build();

        PriorityAssessment savedAssessment = priorityAssessmentRepository.save(assessment);

        // WebSocket 브로드캐스트
        PriorityScoreHistoryDto scoreDto = PriorityScoreHistoryDto.from(savedAssessment);
        webSocketService.broadcastPriorityScoreUpdate(survivor.getId(), scoreDto);

        log.info("Created PriorityAssessment for survivor #{} with final risk score: {}, urgency: {}",
                survivor.getSurvivorNumber(), finalRiskScore, savedAssessment.getUrgencyLevel());

        return savedAssessment;
    }

    // AI 분석 결과를 기반으로 상태 점수와 환경 점수를 계산
    private ScoreResult calculateRiskScore(AIDetectionResultDto.DetectionObject humanDetection,
                                            List<AIDetectionResultDto.DetectionObject> allDetections,
                                            AIDetectionResultDto.DetectionSummary summary) {

        // (A) 피해자 상태 점수 계산
        double statusScore = calculateStatusScore(humanDetection);

        // (B) 환경 위험 승수 계산
        double environmentMultiplier = calculateEnvironmentMultiplier(humanDetection, allDetections, summary);

        return new ScoreResult(statusScore, environmentMultiplier);
    }

    // (A) 피해자 상태 점수 계산 (Pose Estimation 기반)
    // AI 모델 클래스: ["Crawling", "Falling", "Sitting", "Standing"]
    private double calculateStatusScore(AIDetectionResultDto.DetectionObject humanDetection) {
        String pose = humanDetection.getPose();

        if (pose == null) {
            return 3.0; // Pose 정보 없으면 기본값 (서 있음)
        }

        return switch (pose.toLowerCase()) {
            case "falling", "fall", "fallen", "lying" -> 10.0;   // 쓰러져 있음 (Falling)
            case "crawling" -> 8.0;                      // 기어가고 있음 (Crawling)
            case "sitting" -> 5.0;                       // 앉아 있음 (Sitting)
            case "standing" -> 3.0;                      // 서 있음 (Standing)
            default -> 3.0;
        };
    }

    // (B) 환경 위험 승수 계산 (Spatial Analysis 기반)
    private double calculateEnvironmentMultiplier(AIDetectionResultDto.DetectionObject humanDetection,
                                                   List<AIDetectionResultDto.DetectionObject> allDetections,
                                                   AIDetectionResultDto.DetectionSummary summary) {
        // 1. 피해자/침대에 직접 화재 (x 3.0) - fire와 human 박스가 겹침
        if (checkFireOverlapHuman(humanDetection, allDetections)) {
            return 3.0;
        }

        // 2. 짙은 연기 감지 (x 2.0) - smoke 박스의 면적이 전체 화면의 50% 이상
        if (checkDenseSmoke(allDetections)) {
            return 2.0;
        }

        // 3. 방 전체로 화재 확산 (x 1.5) - fire 박스의 면적이 전체 화면의 30% 이상
        if (checkLargeFireArea(allDetections)) {
            return 1.5;
        }

        // 4, 5, 6 판단을 위한 변수
        boolean fireDetected = summary.getFireCount() != null && summary.getFireCount() > 0;
        boolean smallFire = checkSmallFire(allDetections);

        // 4. 단순 화재 감지 - 국소적 (x 1.0) - fire 박스가 감지되었으나, 위 조건에 해당하지 않음 (5% 이상 30% 미만)
        if (fireDetected && !smallFire) {
            return 1.0;
        }

        // 5. 화재가 물체에 국한 (x 0.5) - fire 박스의 면적이 전체 화면의 5% 미만
        if (fireDetected && smallFire) {
            return 0.5;
        }

        // 6. 화재 미감지 (x 0.1) - fire 박스가 감지되지 않음
        return 0.1;
    }

    // 생존자 바운딩 박스와 화재 바운딩 박스의 겹침 여부 확인
    private boolean checkFireOverlapHuman(AIDetectionResultDto.DetectionObject humanDetection,
                                           List<AIDetectionResultDto.DetectionObject> allDetections) {
        AIDetectionResultDto.BoundingBox humanBox = humanDetection.getBox();
        if (humanBox == null) return false;

        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if ("fire".equalsIgnoreCase(detection.getClassName())) {
                AIDetectionResultDto.BoundingBox fireBox = detection.getBox();
                if (fireBox == null) continue;

                double overlapRatio = calculateOverlapRatio(humanBox, fireBox);
                if (overlapRatio > 0.089) { // WeightScore.png 기준
                    return true;
                }
            }
        }
        return false;
    }

    // 두 바운딩 박스의 겹침 비율 계산 (IoU)
    private double calculateOverlapRatio(AIDetectionResultDto.BoundingBox box1,
                                          AIDetectionResultDto.BoundingBox box2) {
        int x1 = Math.max(box1.getX1(), box2.getX1());
        int y1 = Math.max(box1.getY1(), box2.getY1());
        int x2 = Math.min(box1.getX2(), box2.getX2());
        int y2 = Math.min(box1.getY2(), box2.getY2());

        if (x2 <= x1 || y2 <= y1) {
            return 0.0;
        }

        int intersectionArea = (x2 - x1) * (y2 - y1);
        int box1Area = (box1.getX2() - box1.getX1()) * (box1.getY2() - box1.getY1());
        int box2Area = (box2.getX2() - box2.getX1()) * (box2.getY2() - box2.getY1());
        int unionArea = box1Area + box2Area - intersectionArea;

        return unionArea > 0 ? (double) intersectionArea / unionArea : 0.0;
    }

    // 짙은 연기 감지 (smoke 박스 면적 합이 화면의 50% 이상)
    private boolean checkDenseSmoke(List<AIDetectionResultDto.DetectionObject> allDetections) {
        final int TOTAL_AREA = 1920 * 1080;
        final double THRESHOLD = 0.5;

        int smokeTotalArea = 0;
        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if ("smoke".equalsIgnoreCase(detection.getClassName())) {
                AIDetectionResultDto.BoundingBox box = detection.getBox();
                if (box != null) {
                    int area = (box.getX2() - box.getX1()) * (box.getY2() - box.getY1());
                    smokeTotalArea += area;
                }
            }
        }

        return (double) smokeTotalArea / TOTAL_AREA >= THRESHOLD;
    }

    // 방 전체로 화재 확산 (fire 박스 면적 합이 화면의 30% 이상)
    private boolean checkLargeFireArea(List<AIDetectionResultDto.DetectionObject> allDetections) {
        final int TOTAL_AREA = 1920 * 1080;
        final double THRESHOLD = 0.3;

        int fireTotalArea = 0;
        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if ("fire".equalsIgnoreCase(detection.getClassName())) {
                AIDetectionResultDto.BoundingBox box = detection.getBox();
                if (box != null) {
                    int area = (box.getX2() - box.getX1()) * (box.getY2() - box.getY1());
                    fireTotalArea += area;
                }
            }
        }

        return (double) fireTotalArea / TOTAL_AREA >= THRESHOLD;
    }

    // 화재가 물체에 국한 (fire 박스 면적이 화면의 5% 미만)
    private boolean checkSmallFire(List<AIDetectionResultDto.DetectionObject> allDetections) {
        final int TOTAL_AREA = 1920 * 1080;
        final double THRESHOLD = 0.05;

        int fireTotalArea = 0;
        for (AIDetectionResultDto.DetectionObject detection : allDetections) {
            if ("fire".equalsIgnoreCase(detection.getClassName())) {
                AIDetectionResultDto.BoundingBox box = detection.getBox();
                if (box != null) {
                    int area = (box.getX2() - box.getX1()) * (box.getY2() - box.getY1());
                    fireTotalArea += area;
                }
            }
        }

        return fireTotalArea > 0 && (double) fireTotalArea / TOTAL_AREA < THRESHOLD;
    }

    // 반환값 간소화를 위해 static 클래스 생성함
    @Getter
    @RequiredArgsConstructor
    public static class ScoreResult {
        private final double statusScore;
        private final double environmentMultiplier;
    }

}