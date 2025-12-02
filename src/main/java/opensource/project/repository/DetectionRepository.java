package opensource.project.repository;

import opensource.project.domain.Detection;
import opensource.project.domain.enums.DetectionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
