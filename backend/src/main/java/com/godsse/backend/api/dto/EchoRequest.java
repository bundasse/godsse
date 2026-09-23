package com.godsse.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/hello/echo} 요청.
 *
 * @param name    이름 (필수)
 * @param message 메시지 (필수, 2자 이상 100자 이하)
 */
public record EchoRequest(@NotBlank(message = "name 은 필수입니다.") String name,
		@NotBlank(message = "message 는 필수입니다.")
		@Size(min = 2, max = 100, message = "message 는 2자 이상 100자 이하여야 합니다.") String message) {
}
