package opensource.project.service;

import lombok.RequiredArgsConstructor;
import opensource.project.domain.CCTV;
import opensource.project.domain.Location;
import opensource.project.dto.CCTVRequestDto;
import opensource.project.dto.CCTVResponseDto;
import opensource.project.repository.CCTVRepository;
import opensource.project.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CCTVServiceImpl implements CCTVService {

    private final CCTVRepository cctvRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public CCTVResponseDto createCCTV(CCTVRequestDto requestDto) {
        // CCTV 코드가 이미 존재하는지 확인
        if (cctvRepository.findByCctvCode(requestDto.getCctvCode()).isPresent()) {
            throw new IllegalArgumentException("CCTV code already exists: " + requestDto.getCctvCode());
        }

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        CCTV cctv = CCTV.builder()
                .cameraNumber(requestDto.getCameraNumber())
                .cctvCode(requestDto.getCctvCode())
                .status(requestDto.getStatus())
                .location(location)
                .isActive(requestDto.getIsActive())
                .lastActiveAt(requestDto.getIsActive() ? LocalDateTime.now() : null)
                .build();

        CCTV savedCCTV = cctvRepository.save(cctv);
        return CCTVResponseDto.from(savedCCTV);
    }

    @Override
    public List<CCTVResponseDto> getAllCCTVs() {
        return cctvRepository.findAll().stream()
                .map(CCTVResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public CCTVResponseDto getCCTV(Long id) {
        CCTV cctv = cctvRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found with id: " + id));
        return CCTVResponseDto.from(cctv);
    }

    @Override
    @Transactional
    public CCTVResponseDto updateCCTV(Long id, CCTVRequestDto requestDto) {
        CCTV cctv = cctvRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CCTV not found with id: " + id));

        // 변경 중인 CCTV 코드가 중복되는지 확인
        if (!cctv.getCctvCode().equals(requestDto.getCctvCode())) {
            if (cctvRepository.findByCctvCode(requestDto.getCctvCode()).isPresent()) {
                throw new IllegalArgumentException("CCTV code already exists: " + requestDto.getCctvCode());
            }
        }

        Location location = locationRepository.findById(requestDto.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + requestDto.getLocationId()));

        cctv.setCameraNumber(requestDto.getCameraNumber());
        cctv.setCctvCode(requestDto.getCctvCode());
        cctv.setStatus(requestDto.getStatus());
        cctv.setLocation(location);

        // 상태가 활성으로 변경된 경우 lastActiveAt 업데이트
        if (requestDto.getIsActive() && !cctv.getIsActive()) {
            cctv.setLastActiveAt(LocalDateTime.now());
        }
        cctv.setIsActive(requestDto.getIsActive());

        return CCTVResponseDto.from(cctv);
    }

    @Override
    @Transactional
    public void deleteCCTV(Long id) {
        if (!cctvRepository.existsById(id)) {
            throw new IllegalArgumentException("CCTV not found with id: " + id);
        }
        cctvRepository.deleteById(id);
    }

}