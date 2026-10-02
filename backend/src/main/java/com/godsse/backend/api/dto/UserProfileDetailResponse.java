package com.godsse.backend.api.dto;

import java.time.Instant;

import com.godsse.backend.domain.User;

/**
 * 프로필 화면({@code /u/핸들})용 응답.
 *
 * <p>
 * {@link UserProfileResponse}(내 계정 정보)에 프로필 화면에 필요한 숫자와 "조회자의 팔로우 여부"를 더한 형태다.
 * 이 값들은 조회가 한 번 더 필요하므로 로그인 응답에는 넣지 않고 여기서만 계산한다.
 *
 * @param id                  사용자 번호
 * @param handle              고유 핸들
 * @param nickname            화면에 보이는 이름
 * @param bio                 자기소개(없으면 null)
 * @param profileImageUrl     프로필 이미지 경로(없으면 null)
 * @param createdAt           가입 시각
 * @param receivedReviewCount 이 사람이 받은 감상 수
 * @param writtenReviewCount  이 사람이 쓴 감상 수
 * @param followerCount       이 사람을 팔로우하는 사람 수
 * @param followingCount      이 사람이 팔로우하는 사람 수
 * @param isFollowing         조회자가 이 사람을 팔로우하고 있는지 (비로그인이면 false)
 */
public record UserProfileDetailResponse(Long id, String handle, String nickname, String bio, String profileImageUrl,
		Instant createdAt, long receivedReviewCount, long writtenReviewCount, long followerCount,
		long followingCount, boolean isFollowing) {

	public static UserProfileDetailResponse of(User user, long receivedReviewCount, long writtenReviewCount,
			long followerCount, long followingCount, boolean isFollowing) {
		return new UserProfileDetailResponse(user.getId(), user.getHandle(), user.getNickname(), user.getBio(),
				user.getProfileImageUrl(), user.getCreatedAt(), receivedReviewCount, writtenReviewCount,
				followerCount, followingCount, isFollowing);
	}

}
