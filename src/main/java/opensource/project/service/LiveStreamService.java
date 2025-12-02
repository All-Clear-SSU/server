package opensource.project.service;

import opensource.project.dto.LiveStreamResponseDto;
import opensource.project.dto.LiveStreamStartRequestDto;
import opensource.project.dto.LiveStreamStatusResponseDto;

/**
 * 라이브 스트리밍 서비스 인터페이스
 */
public interface LiveStreamService {

    /**
     * 라이브 스트리밍을 시작함
     * @param requestDto 시작 요청 정보
     * @return 스트리밍 시작 결과
     */
    LiveStreamResponseDto startLiveStream(LiveStreamStartRequestDto requestDto);

    /**
     * 라이브 스트리밍을 중지함
     * @param cctvId CCTV ID
     * @return 스트리밍 중지 결과
     */
    LiveStreamResponseDto stopLiveStream(Long cctvId);

    /**
     * 라이브 스트리밍 상태를 조회함
     * @param cctvId CCTV ID
     * @return 스트리밍 상태 정보
     */
    LiveStreamStatusResponseDto getStreamStatus(Long cctvId);

    /**
     * HLS 재생 URL을 조회함
     * @param cctvId CCTV ID
     * @return HLS 재생 URL
     */
    String getHlsUrl(Long cctvId);
}