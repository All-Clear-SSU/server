package opensource.project.repository;

import opensource.project.domain.WifiSensor;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
