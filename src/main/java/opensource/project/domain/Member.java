package opensource.project.domain;

import jakarta.persistence.*;
import lombok.*;

// Member 엔티티 - 시스템 사용자(관리자) 정보, 수동 ID 할당 방식 사용
@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    private Long id;

    @Column(nullable = true, length = 50)
    private String name;
}