package opensource.project.dto;

import lombok.*;
import opensource.project.domain.PriorityAssessment;
import opensource.project.domain.enums.UrgencyLevel;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityAssessmentResponseDto {

    private Long id;
    private Long survivorId;
    private SurvivorResponseDto survivor;
    private Long detectionId;
    private DetectionResponseDto detection;
    private LocalDateTime assessedAt;
    private Double statusScore;
    private Double environmentScore;
    private Double confidenceCoefficient;
    private Double finalRiskScore;
    private UrgencyLevel urgencyLevel;
    private String calculationFormula;
    private String aiModelVersion;
    private String notes;
    private LocalDateTime createdAt;

    public static PriorityAssessmentResponseDto from(PriorityAssessment assessment) {
        return PriorityAssessmentResponseDto.builder()
                .id(assessment.getId())
                .survivorId(assessment.getSurvivor().getId())
                .survivor(SurvivorResponseDto.from(assessment.getSurvivor()))
                .detectionId(assessment.getDetection().getId())
                .detection(DetectionResponseDto.from(assessment.getDetection()))
                .assessedAt(assessment.getAssessedAt())
                .statusScore(assessment.getStatusScore())
                .environmentScore(assessment.getEnvironmentScore())
                .confidenceCoefficient(assessment.getConfidenceCoefficient())
                .finalRiskScore(assessment.getFinalRiskScore())
                .urgencyLevel(assessment.getUrgencyLevel())
                .calculationFormula(assessment.getCalculationFormula())
                .aiModelVersion(assessment.getAiModelVersion())
                .notes(assessment.getNotes())
                .createdAt(assessment.getCreatedAt())
                .build();
    }

    public static PriorityAssessmentResponseDto fromWithoutRelations(PriorityAssessment assessment) {
        return PriorityAssessmentResponseDto.builder()
                .id(assessment.getId())
                .survivorId(assessment.getSurvivor().getId())
                .detectionId(assessment.getDetection().getId())
                .assessedAt(assessment.getAssessedAt())
                .statusScore(assessment.getStatusScore())
                .environmentScore(assessment.getEnvironmentScore())
                .confidenceCoefficient(assessment.getConfidenceCoefficient())
                .finalRiskScore(assessment.getFinalRiskScore())
                .urgencyLevel(assessment.getUrgencyLevel())
                .calculationFormula(assessment.getCalculationFormula())
                .aiModelVersion(assessment.getAiModelVersion())
                .notes(assessment.getNotes())
                .createdAt(assessment.getCreatedAt())
                .build();
    }
}
