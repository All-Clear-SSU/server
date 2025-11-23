package opensource.project.repository;

import opensource.project.domain.WifiSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WifiSensorRepository extends JpaRepository<WifiSensor, Long> {

    // WiFi 센서 ID로 정보 조회
    Optional<WifiSensor> findBySensorCode(String sensorCode);
}
