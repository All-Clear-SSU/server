package opensource.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityAssessmentRequestDto {

    @NotNull(message = "생존자 ID는 필수입니다")
    private Long survivorId;

    @NotNull(message = "탐지 ID는 필수입니다")
    private Long detectionId;

    @NotNull(message = "평가 시각은 필수입니다")
    private LocalDateTime assessedAt;

    @NotNull(message = "상태 점수는 필수입니다")
    private Double statusScore;

    @NotNull(message = "환경 점수는 필수입니다")
    private Double environmentScore;

    @NotNull(message = "신뢰도 계수는 필수입니다")
    private Double confidenceCoefficient;

    @NotNull(message = "최종 위험도는 필수입니다")
    private Double finalRiskScore;

    private String calculationFormula;

    private String aiModelVersion;

    private String notes;
}
