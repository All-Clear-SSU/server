package opensource.project.repository;

import opensource.project.domain.Detection;
import opensource.project.domain.enums.DetectionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DetectionRepository extends JpaRepository<Detection, Long> {

    /**
     * 특정 생존자의 모든 Detection을 감지 시간 내림차순으로 조회함
     *
     * @param survivorId 생존자 ID
     * @return Detection 목록
     */
    List<Detection> findBySurvivorIdOrderByDetectedAtDesc(Long survivorId);

    /**
     * 특정 생존자의 특정 CCTV에서의 Detection을 감지 시간 내림차순으로 조회함
     *
     * @param survivorId 생존자 ID
     * @param cctvId CCTV ID
     * @return Detection 목록
     */
    List<Detection> findBySurvivorIdAndCctvIdOrderByDetectedAtDesc(Long survivorId, Long cctvId);

    /**
     * [추가] 특정 WiFi 센서의 최근 N개 Detection을 조회함
     * WiFi 센서로 탐지된 기록만 조회하며, 감지 시간 기준 내림차순으로 정렬함
     * 페이지 로드 시 그래프 초기 데이터를 가져오는 데 사용함
     *
     * @param wifiSensorId WiFi 센서 ID
     * @param detectionType 탐지 타입 (DetectionType.WIFI)
     * @param pageable 페이징 정보 (limit 설정용)
     * @return Detection 목록 (최신순)
     */
    List<Detection> findByWifiSensorIdAndDetectionTypeOrderByDetectedAtDesc(
            Long wifiSensorId,
            DetectionType detectionType,
            Pageable pageable
    );

    /**
     * [추가] Detection을 모든 관계와 함께 조회하는 메서드
     * 비동기 스레드에서 LazyInitializationException을 방지하기 위해 사용
     *
     * @param id Detection ID
     * @return 모든 관계가 로드된 Detection Optional
     */
    @Query("SELECT d FROM Detection d " +
           "LEFT JOIN FETCH d.survivor " +
           "LEFT JOIN FETCH d.wifiSensor w " +
           "LEFT JOIN FETCH w.location " +
           "LEFT JOIN FETCH d.cctv " +
           "LEFT JOIN FETCH d.location " +
           "WHERE d.id = :id")
    Optional<Detection> findByIdWithRelations(@Param("id") Long id);

    /**
     * 특정 생존자의 모든 Detection 삭제
     *
     * @param survivorId 생존자 ID
     */
    void deleteBySurvivorId(Long survivorId);
}
