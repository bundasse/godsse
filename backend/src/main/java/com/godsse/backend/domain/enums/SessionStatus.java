package com.godsse.backend.domain.enums;

/**
 * 달력에 기록한 세션 일정의 상태.
 *
 * <ul>
 * <li>{@link #PLANNED} — 예정된 세션.</li>
 * <li>{@link #DONE} — 플레이를 마친 세션(리뷰 작성으로 이어질 수 있다).</li>
 * <li>{@link #CANCELED} — 취소된 세션.</li>
 * </ul>
 */
public enum SessionStatus {
	PLANNED,
	DONE,
	CANCELED
}
