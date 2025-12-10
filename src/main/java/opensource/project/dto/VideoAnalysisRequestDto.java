package opensource.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로컬 동영상 파일 분석 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalysisRequestDto {

    /**
     * 동영상 파일 경로 (필수)
     * EC2 서버의 절대 경로 (예: /home/ubuntu/videos/sample.mp4)
     */
    @NotBlank(message = "동영상 파일 경로는 필수입니다")
    private String videoPath;

    /**
     * CCTV ID (필수)
     */
    @NotNull(message = "CCTV ID는 필수입니다")
    private Long cctvId;

    /**
     * 위치 ID (필수)
     */
    @NotNull(message = "위치 ID는 필수입니다")
    private Long locationId;

    /**
     * 객체 탐지 신뢰도 임계값 (선택, 기본값 FastAPI에서 설정)
     */
    private Double confThreshold;

    /**
     * 자세 분류 신뢰도 임계값 (선택, 기본값 FastAPI에서 설정)
     */
    private Double poseConfThreshold;
}