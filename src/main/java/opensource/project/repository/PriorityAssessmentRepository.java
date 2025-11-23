package opensource.project.repository;

import opensource.project.domain.PriorityAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PriorityAssessmentRepository extends JpaRepository<PriorityAssessment, Long> {

    // 특정 생존자의 가장 최근 분석 점수 조회 (First 키워드로 1개만 조회)
    Optional<PriorityAssessment> findFirstBySurvivor_IdOrderByAssessedAtDesc(Long survivorId);

}
