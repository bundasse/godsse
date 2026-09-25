package com.godsse.backend.api.dto;

import java.time.Instant;

import com.godsse.backend.domain.User;

/**
 * 사용자 프로필 응답(단건).
 *
 * <p>
 * 회원가입·로그인·내 정보 조회가 모두 이 형태를 돌려준다.
 *
 * @param id              사용자 번호
 * @param handle          고유 핸들
 * @param nickname        화면에 보이는 이름
 * @param bio             자기소개(없으면 null)
 * @param profileImageUrl 프로필 이미지 경로(없으면 null)
 * @param createdAt       가입 시각
 */
public record UserProfileResponse(Long id, String handle, String nickname, String bio, String profileImageUrl,
		Instant createdAt) {

	/** 엔티티를 응답 DTO 로 바꾼다. (비밀번호는 절대 담지 않는다) */
	public static UserProfileResponse of(User user) {
		return new UserProfileResponse(user.getId(), user.getHandle(), user.getNickname(), user.getBio(),
				user.getProfileImageUrl(), user.getCreatedAt());
	}

}
