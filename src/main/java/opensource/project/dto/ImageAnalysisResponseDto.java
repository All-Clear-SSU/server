package opensource.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAnalysisResponseDto {
    // Detection 엔터티에서 가져온 Object Detection 결과
    private Integer fireCount;
    private Integer humanCount;
    private Integer smokeCount;
    private Integer totalObjects;
    private String survivorStatusMessage;

    // PriorityAssessment 엔터티에서 가져온 결과 (현재는 더미 데이터)
    private Double statusScore;
    private Double environmentScore;
    private Double confidenceCoefficient;
    private Double finalRiskScore;

    // 저장된 엔터티 ID
    private Long detectionId;
    private Long priorityAssessmentId;

    // 분석 이미지 (Base64 인코딩된 문자열)
    private String analyzedImageBase64;
}