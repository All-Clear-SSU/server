package opensource.project.domain.enums;

/**
 * 생존자 삭제 사유를 구분하여 타임아웃 시 최근 기록을 보존하기 위함.
 */
public enum DeleteReason {
    TIMEOUT,
    MANUAL
}
