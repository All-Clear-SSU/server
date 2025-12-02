package opensource.project.repository;

import opensource.project.domain.Location;
import opensource.project.domain.Survivor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurvivorRepository extends JpaRepository<Survivor, Long> {

    Optional<Survivor> findBySurvivorNumber(Integer survivorNumber);

    /**
     * 현재 저장된 survivor_number의 최대값을 반환함
     *
     * @return 최대 생존자 번호 (데이터가 없으면 null)
     */
    @Query("SELECT MAX(s.survivorNumber) FROM Survivor s")
    Integer findMaxSurvivorNumber();

    /**
     * 동일 위치에서 활성 상태인 생존자를 조회함
     * 구조 대기(WAITING) 상태이고 마지막 탐지 시각 기준 내림차순으로 정렬함
     *
     * @param locationId 위치 ID
     * @return 활성 생존자 목록
     */
    @Query("SELECT s FROM Survivor s WHERE s.location.id = :locationId " +
           "AND s.rescueStatus = 'WAITING' " +
           "ORDER BY s.lastDetectedAt DESC")
    List<Survivor> findActiveByLocation(Long locationId);

    /**
     * [추가] 특정 위치에서 특정 시각 이후에 탐지된 활성 생존자를 조회함
     * WiFi 센서로 생존자를 매칭할 때 사용함
     * 최근 N분 이내에 같은 위치에서 탐지된 생존자가 있는지 확인하기 위한 메서드
     *
     * @param location 위치 엔티티
     * @param timeThreshold 시간 임계값 (이 시각 이후에 탐지된 생존자만 조회)
     * @return 조건에 맞는 생존자 목록 (마지막 탐지 시각 기준 내림차순)
     */
    List<Survivor> findByLocationAndLastDetectedAtAfterAndIsActiveTrueOrderByLastDetectedAtDesc(
            Location location,
            LocalDateTime timeThreshold
    );
}
