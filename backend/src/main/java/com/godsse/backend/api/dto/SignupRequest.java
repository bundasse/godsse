package com.godsse.backend.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/auth/signup} 요청.
 *
 * <p>
 * 검증에 실패하면 400 과 함께 어느 필드가 왜 잘못되었는지 내려간다.
 * (프론트는 그 값을 입력칸 옆에 표시한다)
 *
 * @param handle   고유 핸들(아이디 역할). 영문·숫자·밑줄 3~20자
 * @param email    이메일(중복 불가)
 * @param password 비밀번호(8자 이상 72자 이하)
 * @param nickname 화면에 보이는 이름(20자 이하, 중복 허용)
 */
public record SignupRequest(
		@NotBlank(message = "핸들은 필수입니다.")
		@Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "핸들은 영문·숫자·밑줄 3~20자여야 합니다.") String handle,

		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 100, message = "이메일은 100자 이하여야 합니다.") String email,

		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.") String password,

		@NotBlank(message = "닉네임은 필수입니다.")
		@Size(max = 20, message = "닉네임은 20자 이하여야 합니다.") String nickname) {
}
