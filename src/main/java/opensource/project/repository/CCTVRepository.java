package opensource.project.repository;

import opensource.project.domain.CCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CCTVRepository extends JpaRepository<CCTV, Long> {

    // CCTV ID로 CCTV 정보 조회
    Optional<CCTV> findByCctvCode(String cctvCode);

}
