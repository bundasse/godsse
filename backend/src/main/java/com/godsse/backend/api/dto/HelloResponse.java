package com.godsse.backend.api.dto;

import java.time.Instant;

/**
 * {@code GET /api/hello} 응답.
 *
 * @param message   서버가 보내는 메시지
 * @param timestamp 응답 생성 시각 (ISO-8601)
 */
public record HelloResponse(String message, Instant timestamp) {
}
