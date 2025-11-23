package opensource.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UrgencyLevel {
    CRITICAL("매우 위급"),
    HIGH("위급"),
    MEDIUM("보통"),
    LOW("낮음");

    private final String description;
}
