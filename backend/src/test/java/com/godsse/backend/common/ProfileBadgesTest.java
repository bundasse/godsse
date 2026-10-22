package com.godsse.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 프로필 배지 계산 규칙.
 *
 * <p>
 * DB 가 필요 없는 순수 함수라 스프링 없이 빠르게 확인한다.
 */
class ProfileBadgesTest {

	@Test
	@DisplayName("활동이 없으면 배지도 없다")
	void noActivity() {
		assertThat(ProfileBadges.of(0, 0, 0, 0)).isEmpty();
	}

	@Test
	@DisplayName("감상을 처음 쓰면 첫 감상 배지가 붙는다")
	void firstReview() {
		assertThat(ProfileBadges.of(1, 0, 0, 0)).containsExactly(ProfileBadges.FIRST_REVIEW);
	}

	@Test
	@DisplayName("기준을 넘긴 배지만 정해진 순서대로 붙는다")
	void allBadges() {
		assertThat(ProfileBadges.of(10, 10, 10, 10)).containsExactly(ProfileBadges.FIRST_REVIEW,
				ProfileBadges.REVIEW_10, ProfileBadges.RECEIVED_10, ProfileBadges.HEARTS_10,
				ProfileBadges.FOLLOWERS_10);
	}

	@Test
	@DisplayName("기준에 못 미치면 그 배지는 붙지 않는다")
	void belowThreshold() {
		assertThat(ProfileBadges.of(9, 9, 9, 9)).containsExactly(ProfileBadges.FIRST_REVIEW);
	}

}
