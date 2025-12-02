package opensource.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 라이브 스트리밍 상태 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStreamStatusResponseDto {

    /**
     * CCTV ID
     */
    private Long cctvId;

    /**
     * 스트리밍 진행 여부
     */
    private Boolean isStreaming;

    /**
     * RTSP URL
     */
    private String rtspUrl;

    /**
     * HLS 재생 URL
     */
    private String hlsUrl;

    /**
     * 스트리밍 시작 시간
     */
    private String startedAt;

    /**
     * 처리된 프레임 수
     */
    private Integer frameCount;

    /**
     * CCTV 이름
     */
    private String cctvName;

    /**
     * 위치 정보
     */
    private String location;
}