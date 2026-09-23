package com.godsse.backend.api.dto;

import java.util.List;

/**
 * 요청 검증 실패 응답 (HTTP 400).
 *
 * @param message 전체 오류 메시지
 * @param errors  필드별 오류 목록
 */
public record ValidationErrorResponse(String message, List<FieldValidationError> errors) {
}
