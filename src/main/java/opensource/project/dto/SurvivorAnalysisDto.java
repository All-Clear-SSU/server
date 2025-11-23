package opensource.project.dto;

import lombok.*;
import opensource.project.domain.Detection;
import opensource.project.domain.PriorityAssessment;
import opensource.project.domain.Survivor;
import opensource.project.domain.enums.CurrentStatus;
import opensource.project.domain.enums.DetectionMethod;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurvivorAnalysisDto {

    // 생존자 기본 정보
    private Long survivorId;
    private Integer survivorNumber;

    // AI 분석 결과 (텍스트 설명)
    private String aiAnalysisResult;

    // 위치 정보
    private Long locationId;
    private String fullAddress;

    // 생존자 상태
    private CurrentStatus currentStatus;
    private String currentStatusDescription;

    // 탐지 수단
    private DetectionMethod detectionMethod;
    private String detectionMethodDescription;

    // 위험도 점수 (PriorityAssessment)
    private Double statusScore;
    private Double environmentScore;
    private Double confidenceCoefficient;
    private Double finalRiskScore;

    // Survivor, Detection, PriorityAssessment를 통합하여 DTO 생성
    public static SurvivorAnalysisDto from(Survivor survivor, Detection latestDetection, PriorityAssessment assessment) {
        SurvivorAnalysisDtoBuilder builder = SurvivorAnalysisDto.builder()
                .survivorId(survivor.getId())
                .survivorNumber(survivor.getSurvivorNumber())
                .locationId(survivor.getLocation().getId())
                .fullAddress(survivor.getLocation().getFullAddress())
                .currentStatus(survivor.getCurrentStatus())
                .currentStatusDescription(survivor.getCurrentStatus().getDescription())
                .detectionMethod(survivor.getDetectionMethod())
                .detectionMethodDescription(survivor.getDetectionMethod().getDescription());

        // 최신 Detection이 있는 경우 AI 분석 결과 추가
        if (latestDetection != null) {
            builder.aiAnalysisResult(latestDetection.getAiAnalysisResult());
        }

        // PriorityAssessment가 있는 경우 위험도 점수 추가
        if (assessment != null) {
            builder.statusScore(assessment.getStatusScore())
                    .environmentScore(assessment.getEnvironmentScore())
                    .confidenceCoefficient(assessment.getConfidenceCoefficient())
                    .finalRiskScore(assessment.getFinalRiskScore());
        }

        return builder.build();
    }
}