package com.godsse.backend.domain.enums;

/**
 * 알림의 종류.
 *
 * <ul>
 * <li>{@link #REVIEW} — 내가 감상을 받았다.</li>
 * <li>{@link #LIKE} — 내가 쓴 감상에 좋아요가 달렸다.</li>
 * <li>{@link #REPLY} — 내가 쓴(또는 내가 받은) 감상에 답장이 달렸다.</li>
 * <li>{@link #FOLLOW} — 누군가 나를 팔로우했다.</li>
 * </ul>
 */
public enum NotificationType {
	REVIEW,
	LIKE,
	REPLY,
	FOLLOW
}
