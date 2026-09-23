package com.godsse.backend.api.dto;

/**
 * 필드 단위 검증 실패 정보.
 *
 * @param field  검증에 실패한 필드명
 * @param reason 실패 사유
 */
public record FieldValidationError(String field, String reason) {
}
