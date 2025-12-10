package opensource.project.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.AIDetectionResultDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 위험도 점수 계산 서비스
 * AI 분석 결과를 기반으로 생존자의 위험도 점수를 계산
 */
@Service
@RequiredArgsConstructor
public class RiskScoreCalculator {

    private final EnvironmentalAnalysisService environmentalAnalysisService;

    /**
     * AI 분석 결과를 기반으로 상태 점수와 환경 점수를 계산
     *
     * @param humanDetection 사람 탐지 객체
     * @param allDetections 모든 탐지 객체
     * @param summary 탐지 요약 정보
     * @return 점수 계산 결과
     */
    public ScoreResult calculateRiskScore(AIDetectionResultDto.DetectionObject humanDetection,
                                           List<AIDetectionResultDto.DetectionObject> allDetections,
                                           AIDetectionResultDto.DetectionSummary summary) {

        // (A) 피해자 상태 점수 계산
        double statusScore = calculateStatusScore(humanDetection);

        // (B) 환경 위험 승수 계산
        double environmentMultiplier = calculateEnvironmentMultiplier(humanDetection, allDetections, summary);

        return new ScoreResult(statusScore, environmentMultiplier);
    }

    /**
     * (A) 피해자 상태 점수 계산 (Pose Estimation 기반)
     * AI 모델 클래스: ["Crawling", "Falling", "Sitting", "Standing"]
     */
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

    /**
     * (B) 환경 위험 승수 계산 (Spatial Analysis 기반)
     */
    private double calculateEnvironmentMultiplier(AIDetectionResultDto.DetectionObject humanDetection,
                                                   List<AIDetectionResultDto.DetectionObject> allDetections,
                                                   AIDetectionResultDto.DetectionSummary summary) {
        // 1. 피해자/침대에 직접 화재 (x 3.0) - fire와 human 박스가 겹침
        if (environmentalAnalysisService.checkFireOverlapHuman(humanDetection, allDetections)) {
            return 3.0;
        }

        // 2. 짙은 연기 감지 (x 2.0) - smoke 박스의 면적이 전체 화면의 50% 이상
        if (environmentalAnalysisService.checkDenseSmoke(allDetections)) {
            return 2.0;
        }

        // [대안] 연기가 조금이라도 감지되면 환경 승수 2.0 적용
        // 아래 코드로 변경하면 연기 양과 관계없이 연기 감지 시 즉시 2.0 적용됨
        /*
        boolean smokeDetected = summary.getSmokeCount() != null && summary.getSmokeCount() > 0;
        if (smokeDetected) {
            return 2.0;
        }
        */

        // 3. 방 전체로 화재 확산 (x 1.5) - fire 박스의 면적이 전체 화면의 30% 이상
        if (environmentalAnalysisService.checkLargeFireArea(allDetections)) {
            return 1.5;
        }

        // 4, 5, 6 판단을 위한 변수
        boolean fireDetected = summary.getFireCount() != null && summary.getFireCount() > 0;
        boolean smallFire = environmentalAnalysisService.checkSmallFire(allDetections);

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

    /**
     * 점수 계산 결과를 담는 클래스
     */
    @Getter
    @RequiredArgsConstructor
    public static class ScoreResult {
        private final double statusScore;
        private final double environmentMultiplier;
    }
}