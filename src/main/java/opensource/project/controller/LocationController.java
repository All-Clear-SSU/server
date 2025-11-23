package opensource.project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.LocationRequestDto;
import opensource.project.dto.LocationResponseDto;
import opensource.project.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LocationController {

    private final LocationService locationService;

    // 위치 정보 등록
    @PostMapping
    public ResponseEntity<LocationResponseDto> createLocation(@Valid @RequestBody LocationRequestDto requestDto) {
        LocationResponseDto response = locationService.createLocation(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 모든 위치 정보 조회
    @GetMapping
    public ResponseEntity<List<LocationResponseDto>> getAllLocations() {
        List<LocationResponseDto> locations = locationService.getAllLocations();
        return ResponseEntity.ok(locations);
    }

    // 특정 위치 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<LocationResponseDto> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequestDto requestDto) {
        LocationResponseDto response = locationService.updateLocation(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // 특정 위치 정보 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.ok("Location deleted successfully");
    }

}