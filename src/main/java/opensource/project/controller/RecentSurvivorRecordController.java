package opensource.project.controller;

import lombok.RequiredArgsConstructor;
import opensource.project.dto.RecentSurvivorRecordResponseDto;
import opensource.project.service.RecentSurvivorRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recent-survivors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecentSurvivorRecordController {

    private final RecentSurvivorRecordService recentSurvivorRecordService;

    @GetMapping
    public ResponseEntity<List<RecentSurvivorRecordResponseDto>> getRecentSurvivors(
            @RequestParam(name = "hours", defaultValue = "48") Integer hours
    ) {
        int safeHours = (hours == null || hours <= 0) ? 48 : hours;
        List<RecentSurvivorRecordResponseDto> records = recentSurvivorRecordService.getRecentRecords(safeHours);
        return ResponseEntity.ok(records);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecentRecord(@PathVariable Long id) {
        recentSurvivorRecordService.deleteRecentRecord(id);
        return ResponseEntity.noContent().build();
    }
}
