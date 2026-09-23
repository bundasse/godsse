package com.godsse.backend.api.dto;

/**
 * {@code POST /api/hello/echo} 응답.
 *
 * @param name    요청한 이름
 * @param message 앞뒤 공백을 제거한 메시지
 * @param length  앞뒤 공백을 제거한 메시지 길이
 */
public record EchoResponse(String name, String message, int length) {
}
