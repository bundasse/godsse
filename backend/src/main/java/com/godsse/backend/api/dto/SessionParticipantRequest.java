package com.godsse.backend.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 세션 일정에 "함께한 사람"을 추가하는 요청.
 *
 * @param handle 함께한 사람의 핸들({@code @} 없이)
 */
public record SessionParticipantRequest(@NotBlank(message = "함께한 사람의 핸들은 필수입니다.") String handle) {
}
