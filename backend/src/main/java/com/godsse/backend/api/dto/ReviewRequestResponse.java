package com.godsse.backend.api.dto;

/**
 * 세션 참가자에게 감상을 부탁한 결과.
 *
 * <p>
 * 화면이 "왜 아무도 못 받았는지"를 구분해 안내할 수 있도록 세 가지로 나눠 센다.
 *
 * @param requestedCount 실제로 알림을 보낸 사람 수
 * @param alreadyReviewedCount 이미 나에게 감상을 쓴 사람 수(요청에서 빠진다)
 * @param alreadyRequestedCount 아직 읽지 않은 요청이 있어 <b>건너뛴</b> 사람 수(중복 방지)
 */
public record ReviewRequestResponse(int requestedCount, int alreadyReviewedCount, int alreadyRequestedCount) {
}
