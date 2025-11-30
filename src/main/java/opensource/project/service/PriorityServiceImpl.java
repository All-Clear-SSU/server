package opensource.project.service;

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
    private final RiskScoreCalculator riskScoreCalculator;

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
                .assessedAt(LocalDateTime.now())
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

        // 위험도 점수 계산 (RiskScoreCalculator로 위임)
        RiskScoreCalculator.ScoreResult scoreResult =
                riskScoreCalculator.calculateRiskScore(humanDetection, allDetections, summary);
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
}