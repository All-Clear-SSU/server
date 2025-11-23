package opensource.project.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 분석 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysisRequestDto {
    @Valid
    @NotNull(message = "aiResult must not be null")
    private AIDetectionResultDto aiResult;

    @NotNull(message = "cctvId must not be null")
    private Long cctvId;

    @NotNull(message = "locationId must not be null")
    private Long locationId;

    private String videoUrl;
}