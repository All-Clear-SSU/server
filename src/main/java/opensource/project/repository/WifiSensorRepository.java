package opensource.project.repository;

import opensource.project.domain.WifiSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WifiSensorRepository extends JpaRepository<WifiSensor, Long> {

    /**
     * WiFi 센서 코드로 정보를 조회함
     *
     * @param sensorCode 센서 코드
     * @return WifiSensor Optional
     */
    Optional<WifiSensor> findBySensorCode(String sensorCode);

    /**
     * [추가] 활성 상태인 WiFi 센서 목록을 조회함
     * 대시보드에서 현재 작동 중인 센서만 표시하는 데 사용함
     *
     * @param isActive 활성 상태 (true: 활성, false: 비활성)
     * @return 활성 센서 목록
     */
    List<WifiSensor> findByIsActive(Boolean isActive);

    /**
     * [추가] Location을 함께 조회하는 WiFi 센서 조회 메서드
     * 비동기 스레드에서 LazyInitializationException을 방지하기 위해 사용
     *
     * @param id 센서 ID
     * @return Location과 함께 조회된 WifiSensor Optional
     */
    @Query("SELECT s FROM WifiSensor s LEFT JOIN FETCH s.location WHERE s.id = :id")
    Optional<WifiSensor> findByIdWithLocation(@Param("id") Long id);
}
