package opensource.project.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import opensource.project.dto.BuildingRegisterRequest;
import opensource.project.service.BuildingRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/buildings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BuildingController {

    private final BuildingRegistrationService buildingRegistrationService;

    /**
     * 건물/장비 일괄 등록
     */
    @PostMapping("/register")
    public ResponseEntity<Void> registerBuilding(@Valid @RequestBody BuildingRegisterRequest request) {
        buildingRegistrationService.registerBuilding(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 등록된 건물명 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<String>> getBuildings() {
        return ResponseEntity.ok(buildingRegistrationService.getBuildingNames());
    }
}
