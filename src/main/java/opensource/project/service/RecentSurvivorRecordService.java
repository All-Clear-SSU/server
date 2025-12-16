package opensource.project.service;

import lombok.RequiredArgsConstructor;
import opensource.project.dto.RecentSurvivorRecordResponseDto;
import opensource.project.repository.RecentSurvivorRecordRepository;
import opensource.project.service.WebSocketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentSurvivorRecordService {

    private final RecentSurvivorRecordRepository recentSurvivorRecordRepository;
    private final WebSocketService webSocketService;

    /**
     * 최근 N시간 내 기록을 최신순으로 반환.
     */
    public List<RecentSurvivorRecordResponseDto> getRecentRecords(int hours) {
        LocalDateTime after = LocalDateTime.now().minusHours(hours);
        return recentSurvivorRecordRepository.findByArchivedAtAfterOrderByArchivedAtDesc(after)
                .stream()
                .map(RecentSurvivorRecordResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRecentRecord(Long id) {
        if (!recentSurvivorRecordRepository.existsById(id)) {
            throw new IllegalArgumentException("Recent survivor record not found with id: " + id);
        }
        recentSurvivorRecordRepository.deleteById(id);
        // 프론트 실시간 반영
        webSocketService.broadcastRecentRecordDeleted(id);
    }
}
