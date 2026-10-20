package com.godsse.backend.api.dto;

/**
 * 안 읽은 알림 수. 헤더 배지가 주기적으로 확인하는 값.
 *
 * @param count 안 읽은 알림 수
 */
public record UnreadCountResponse(long count) {
}
