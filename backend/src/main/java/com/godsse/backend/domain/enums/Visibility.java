package com.godsse.backend.domain.enums;

/**
 * 리뷰를 누가 볼 수 있는지 나타낸다.
 *
 * <ul>
 * <li>{@link #PUBLIC} — 누구나 볼 수 있다(기본값).</li>
 * <li>{@link #FOLLOWERS} — 작성자를 팔로우한 사람만 볼 수 있다.</li>
 * <li>{@link #PRIVATE} — 작성자와 받은 사람만 볼 수 있다.</li>
 * </ul>
 */
public enum Visibility {
	PUBLIC,
	FOLLOWERS,
	PRIVATE
}
