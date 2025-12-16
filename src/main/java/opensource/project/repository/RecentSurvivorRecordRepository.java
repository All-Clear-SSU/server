package opensource.project.repository;

import opensource.project.domain.RecentSurvivorRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecentSurvivorRecordRepository extends JpaRepository<RecentSurvivorRecord, Long> {

    /**
     * 주어진 시각 이후에 보관된 스냅샷을 최신순으로 조회.
     */
    List<RecentSurvivorRecord> findByArchivedAtAfterOrderByArchivedAtDesc(LocalDateTime after);
}
