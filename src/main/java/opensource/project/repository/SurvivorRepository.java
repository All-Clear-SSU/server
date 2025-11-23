package opensource.project.repository;

import opensource.project.domain.Survivor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurvivorRepository extends JpaRepository<Survivor, Long> {

    Optional<Survivor> findBySurvivorNumber(Integer survivorNumber);

    // 현재 저장된 survivor_id의 최대값 반환
    @Query("SELECT MAX(s.survivorNumber) FROM Survivor s")
    Integer findMaxSurvivorNumber();

    // 동일 위치 및 CCTV에서 활성 상태인 생존자 조회
    @Query("SELECT s FROM Survivor s WHERE s.location.id = :locationId " +
           "AND s.rescueStatus = 'WAITING' " +
           "ORDER BY s.lastDetectedAt DESC")
    List<Survivor> findActiveByLocation(Long locationId);
}
