package com.godsse.backend.api.dto;

import java.time.Instant;

import com.godsse.backend.domain.User;

/**
 * 프로필 화면({@code /u/핸들})용 응답.
 *
 * <p>
 * {@link UserProfileResponse}(내 계정 정보)에 프로필 화면에 필요한 숫자를 더한 형태다.
 * 리뷰·팔로우 개수는 조회가 한 번 더 필요하므로, 로그인 응답에는 넣지 않고 여기서만 계산한다.
 *
 * @param id                  사용자 번호
 * @param handle              고유 핸들
 * @param nickname            화면에 보이는 이름
 * @param bio                 자기소개(없으면 null)
 * @param profileImageUrl     프로필 이미지 경로(없으면 null)
 * @param createdAt           가입 시각
 * @param receivedReviewCount 이 사람이 받은 감상 수
 * @param writtenReviewCount  이 사람이 쓴 감상 수
 */
public record UserProfileDetailResponse(Long id, String handle, String nickname, String bio, String profileImageUrl,
		Instant createdAt, long receivedReviewCount, long writtenReviewCount) {

	public static UserProfileDetailResponse of(User user, long receivedReviewCount, long writtenReviewCount) {
		return new UserProfileDetailResponse(user.getId(), user.getHandle(), user.getNickname(), user.getBio(),
				user.getProfileImageUrl(), user.getCreatedAt(), receivedReviewCount, writtenReviewCount);
	}

}
