package opensource.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 라이브 스트리밍 시작 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamResponseDto {

    /**
     * 응답 상태 (success/error)
     */
    private String status;

    /**
     * 메시지
     */
    private String message;

    /**
     * CCTV ID
     */
    private Long cctvId;

    /**
     * HLS 재생 URL
     */
    private String hlsUrl;

    /**
     * RTSP 스트림 URL
     */
    private String rtspUrl;
}