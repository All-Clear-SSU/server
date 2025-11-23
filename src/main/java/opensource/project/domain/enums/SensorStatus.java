package opensource.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SensorStatus {
    ACTIVE("활성"),
    INACTIVE("비활성"),
    MAINTENANCE("점검중");

    private final String description;
}
