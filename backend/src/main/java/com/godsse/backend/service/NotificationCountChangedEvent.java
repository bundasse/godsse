package com.godsse.backend.service;

/**
 * "안 읽은 알림 개수가 바뀌었다"는 사실을 담은 사건(이벤트).
 *
 * <p>
 * 알림을 만들거나 읽음 처리한 <b>트랜잭션 안에서</b> 발행되고, 실제 전송(SSE)은 커밋이 끝난 뒤
 * {@link NotificationPushListener} 가 맡는다. 이렇게 나누면 <b>롤백된 변경은 화면에 새어 나가지 않는다</b>.
 *
 * @param userId 받는 사람 id
 * @param count  커밋될 시점의 안 읽은 개수
 */
public record NotificationCountChangedEvent(Long userId, long count) {
}
