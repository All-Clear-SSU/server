package opensource.project.repository;

import opensource.project.domain.Detection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetectionRepository extends JpaRepository<Detection, Long> {

    // 특정 생존자의 모든 Detection을 감지 시간 내림차순으로 조회
    List<Detection> findBySurvivorIdOrderByDetectedAtDesc(Long survivorId);

    // 특정 생존자의 특정 CCTV에서의 Detection을 감지 시간 내림차순으로 조회
    List<Detection> findBySurvivorIdAndCctvIdOrderByDetectedAtDesc(Long survivorId, Long cctvId);

}
