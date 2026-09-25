package com.godsse.backend.api.dto;

import com.godsse.backend.domain.User;

/**
 * 목록에 표시할 사용자 요약 정보. (검색 결과, 팔로워 목록 등)
 *
 * @param handle          고유 핸들
 * @param nickname        화면에 보이는 이름
 * @param profileImageUrl 프로필 이미지 경로(없으면 null)
 */
public record UserSummary(String handle, String nickname, String profileImageUrl) {

	/** 엔티티를 요약 DTO 로 바꾼다. */
	public static UserSummary of(User user) {
		return new UserSummary(user.getHandle(), user.getNickname(), user.getProfileImageUrl());
	}

}
