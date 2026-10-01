package com.godsse.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code PATCH /api/users/me} 요청 — 프로필 편집.
 *
 * <p>
 * 핸들은 주소({@code /u/핸들})에 쓰이므로 바꿀 수 없다. 닉네임과 자기소개만 바꾼다.
 * 프로필 이미지는 별도 API({@code POST /api/users/me/avatar})로 올린다. (파일 전송은 형식이 달라서)
 *
 * @param nickname 화면에 보이는 이름 (필수, 20자 이하)
 * @param bio      자기소개 (선택, 300자 이하)
 */
public record UpdateProfileRequest(@NotBlank(message = "닉네임은 필수입니다.")
		@Size(max = 20, message = "닉네임은 20자 이하여야 합니다.") String nickname,

		@Size(max = 300, message = "자기소개는 300자 이하로 써 주세요.") String bio) {
}
