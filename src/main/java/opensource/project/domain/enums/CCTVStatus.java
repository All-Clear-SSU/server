package opensource.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CCTVStatus {
    LIVE("실시간"),
    REC("녹화중"),
    INACTIVE("비활성");

    private final String description;
}
