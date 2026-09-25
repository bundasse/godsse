package com.godsse.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /api/auth/login} 요청.
 *
 * <p>
 * 로그인 아이디는 핸들(고유 이름)이다.
 *
 * @param handle   가입할 때 정한 핸들
 * @param password 비밀번호(원문)
 */
public record LoginRequest(@NotBlank(message = "핸들은 필수입니다.") String handle,
		@NotBlank(message = "비밀번호는 필수입니다.") String password) {
}
