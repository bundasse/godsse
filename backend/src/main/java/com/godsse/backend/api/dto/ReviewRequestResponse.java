package com.godsse.backend.api.dto;

/**
 * 세션 참가자에게 감상을 부탁한 결과.
 *
 * @param requestedCount 실제로 알림을 보낸 사람 수(이미 감상을 쓴 사람은 빠진다)
 */
public record ReviewRequestResponse(int requestedCount) {
}
