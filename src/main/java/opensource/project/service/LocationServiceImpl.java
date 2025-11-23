package opensource.project.service;

import lombok.RequiredArgsConstructor;
import opensource.project.domain.Location;
import opensource.project.dto.LocationRequestDto;
import opensource.project.dto.LocationResponseDto;
import opensource.project.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public LocationResponseDto createLocation(LocationRequestDto requestDto) {
        Location location = Location.builder()
                .buildingName(requestDto.getBuildingName())
                .floor(requestDto.getFloor())
                .roomNumber(requestDto.getRoomNumber())
                .build();

        Location savedLocation = locationRepository.save(location);
        return LocationResponseDto.from(savedLocation);
    }

    @Override
    public List<LocationResponseDto> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(LocationResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LocationResponseDto updateLocation(Long id, LocationRequestDto requestDto) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found with id: " + id));

        // 위치 정보 수정 시 입력되어야 하는 필수 정보
        location.setBuildingName(requestDto.getBuildingName());
        location.setFloor(requestDto.getFloor());
        location.setRoomNumber(requestDto.getRoomNumber());

        return LocationResponseDto.from(location);
    }

    @Override
    @Transactional
    public void deleteLocation(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new IllegalArgumentException("Location not found with id: " + id);
        }
        locationRepository.deleteById(id);
    }

}