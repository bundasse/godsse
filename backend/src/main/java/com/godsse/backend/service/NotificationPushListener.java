package com.godsse.backend.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 개수가 바뀌었다는 사건을 <b>커밋 이후에</b> 실제 전송(SSE)으로 바꾼다.
 *
 * <p>
 * 왜 바로 보내지 않고 미룰까?
 * <ul>
 * <li>알림을 저장한 트랜잭션이 <b>롤백</b>되면, 화면에는 없는 알림이 잠깐 떴다가 사라진다.</li>
 * <li>커밋 전에는 다른 트랜잭션이 그 데이터를 볼 수 없어 "개수가 1" 이라는 말도 아직 사실이 아니다.</li>
 * </ul>
 *
 * <p>
 * {@code fallbackExecution = true} 는 "트랜잭션 없이 발행된 사건도 그냥 실행하라"는 뜻이다.
 * (예: 앞으로 배치·테스트에서 트랜잭션 밖에서 알림을 만들 때도 동작해야 한다)
 */
@Component
public class NotificationPushListener {

	private final NotificationStream notificationStream;

	public NotificationPushListener(NotificationStream notificationStream) {
		this.notificationStream = notificationStream;
	}

	/** 커밋이 끝난 뒤에만 호출된다. (롤백되면 아무 일도 일어나지 않는다) */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onCountChanged(NotificationCountChangedEvent event) {
		this.notificationStream.publish(event.userId(), event.count());
	}

}
