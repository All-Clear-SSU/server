package opensource.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RescueStatus {
    WAITING("대기"),
    IN_RESCUE("출동중"),
    RESCUED("구조완료"),
    CANCELED("취소");

    private final String description;
}
