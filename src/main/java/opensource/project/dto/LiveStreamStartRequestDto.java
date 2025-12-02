package opensource.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 라이브 스트리밍 시작 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamStartRequestDto {

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