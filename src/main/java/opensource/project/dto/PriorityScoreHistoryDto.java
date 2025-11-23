package opensource.project.dto;

import lombok.*;
import opensource.project.domain.PriorityAssessment;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityScoreHistoryDto {

    private Long assessmentId;
    private LocalDateTime assessedAt;
    private Double statusScore;
    private Double environmentScore;
    private Double confidenceCoefficient;
    private Double finalRiskScore;

    public static PriorityScoreHistoryDto from(PriorityAssessment assessment) {
        return PriorityScoreHistoryDto.builder()
                .assessmentId(assessment.getId())
                .assessedAt(assessment.getAssessedAt())
                .statusScore(assessment.getStatusScore())
                .environmentScore(assessment.getEnvironmentScore())
                .confidenceCoefficient(assessment.getConfidenceCoefficient())
                .finalRiskScore(assessment.getFinalRiskScore())
                .build();
    }
}