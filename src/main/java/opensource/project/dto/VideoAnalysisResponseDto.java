package opensource.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 동영상 파일 분석 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoAnalysisResponseDto {

    /**
     * 처리 상태 (processing, success, error)
     */
    private String status;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * CCTV ID
     */
    private Long cctvId;

    /**
     * 위치 ID
     */
    private Long locationId;

    /**
     * 동영상 파일 경로
     */
    private String videoPath;

    /**
     * HLS 재생 URL
     */
    private String hlsUrl;
}