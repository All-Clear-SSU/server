package opensource.project.service;

import opensource.project.dto.LocationRequestDto;
import opensource.project.dto.LocationResponseDto;

import java.util.List;

/**
 * 위치 관리 서비스 인터페이스
 */
public interface LocationService {

    // 위치 등록
    LocationResponseDto createLocation(LocationRequestDto requestDto);

    // 전체 위치 조회
    List<LocationResponseDto> getAllLocations();

    // 위치 정보 수정
    LocationResponseDto updateLocation(Long id, LocationRequestDto requestDto);

    // 위치 삭제
    void deleteLocation(Long id);
}