package com.godsse.backend.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로필에 붙는 활동 배지.
 *
 * <p>
 * 배지는 <b>저장하지 않는다</b>. 지금 숫자(감상 수·하트 수·팔로워 수)에서 계산하면 언제나 최신이고,
 * 배지 규칙을 바꿔도 DB 를 손댈 필요가 없다. 대신 계산은 조회할 때마다 한 번 더 한다.
 *
 * <p>
 * 서버는 <b>코드</b>({@code FIRST_REVIEW})만 내려주고, 화면에 보일 이름·아이콘은 프론트엔드의
 * {@code constants/badges.js} 가 정한다. 문구를 고치려고 서버를 다시 배포하지 않아도 된다.
 */
public final class ProfileBadges {

	/** 감상을 처음 남겼다. */
	public static final String FIRST_REVIEW = "FIRST_REVIEW";

	/** 감상을 10편 썼다. */
	public static final String REVIEW_10 = "REVIEW_10";

	/** 감상을 10편 받았다. */
	public static final String RECEIVED_10 = "RECEIVED_10";

	/** 내가 쓴 감상이 받은 하트가 10개를 넘었다. */
	public static final String HEARTS_10 = "HEARTS_10";

	/** 팔로워가 10명을 넘었다. */
	public static final String FOLLOWERS_10 = "FOLLOWERS_10";

	private ProfileBadges() {
	}

	/**
	 * 숫자로 배지 목록을 만든다. 순서는 위에 선언한 순서를 지킨다(화면에서 자리가 계속 바뀌지 않게).
	 *
	 * @param writtenReviewCount  쓴 감상 수
	 * @param receivedReviewCount 받은 감상 수
	 * @param receivedLikeCount   내 감상이 받은 하트 총합
	 * @param followerCount       팔로워 수
	 */
	public static List<String> of(long writtenReviewCount, long receivedReviewCount, long receivedLikeCount,
			long followerCount) {
		List<String> badges = new ArrayList<>();

		if (writtenReviewCount >= 1) {
			badges.add(FIRST_REVIEW);
		}
		if (writtenReviewCount >= 10) {
			badges.add(REVIEW_10);
		}
		if (receivedReviewCount >= 10) {
			badges.add(RECEIVED_10);
		}
		if (receivedLikeCount >= 10) {
			badges.add(HEARTS_10);
		}
		if (followerCount >= 10) {
			badges.add(FOLLOWERS_10);
		}

		return List.copyOf(badges);
	}

}
