package opensource.project.service;

import opensource.project.dto.CCTVRequestDto;
import opensource.project.dto.CCTVResponseDto;
import opensource.project.dto.UpdateRtspUrlRequestDto;

import java.util.List;

/**
 * CCTV 관리 서비스 인터페이스
 */
public interface CCTVService {

    // CCTV 등록
    CCTVResponseDto createCCTV(CCTVRequestDto requestDto);

    // 전체 CCTV 조회
    List<CCTVResponseDto> getAllCCTVs();

    // 특정 CCTV 조회
    CCTVResponseDto getCCTV(Long id);

    // CCTV 정보 수정
    CCTVResponseDto updateCCTV(Long id, CCTVRequestDto requestDto);

    // CCTV 삭제
    void deleteCCTV(Long id);

    // CCTV의 RTSP URL만 업데이트함
    CCTVResponseDto updateRtspUrl(Long id, UpdateRtspUrlRequestDto requestDto);
}