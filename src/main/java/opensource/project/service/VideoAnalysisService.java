package opensource.project.service;

import opensource.project.dto.VideoAnalysisRequestDto;
import opensource.project.dto.VideoAnalysisResponseDto;

/**
 * 로컬 동영상 파일 분석 서비스 인터페이스
 */
public interface VideoAnalysisService {

    /**
     * 로컬 동영상 파일 분석을 시작함
     * FastAPI에 분석 요청을 보내고, 백그라운드에서 분석이 진행됨
     *
     * @param requestDto 동영상 분석 요청 정보
     * @return 분석 시작 응답
     */
    VideoAnalysisResponseDto analyzeVideo(VideoAnalysisRequestDto requestDto);
}