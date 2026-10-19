package com.godsse.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/reviews/{id}/replies} 요청 — 답장 작성.
 *
 * @param body 답장 내용(1~500자)
 */
public record ReviewReplyRequest(@NotBlank(message = "답장 내용을 입력해 주세요.")
		@Size(max = 500, message = "답장은 500자 이하여야 합니다.") String body) {
}
