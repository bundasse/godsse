package com.godsse.backend.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 실시간 전송(SSE, Server-Sent Events).
 *
 * <p>
 * 사용자별로 열린 연결({@link SseEmitter})을 들고 있다가, 그 사람의 안 읽은 개수가 바뀌면
 * 숫자를 밀어 넣는다. 화면은 {@code EventSource} 로 받아 배지만 다시 그리면 된다.
 *
 * <p>
 * <b>주의할 점 세 가지</b>
 * <ul>
 * <li>연결은 요청 스레드가 끝난 뒤에도 살아 있고 다른 스레드에서 쓰이므로 동시성 컬렉션을 쓴다.</li>
 * <li>끊긴 연결을 목록에 남겨 두면 메모리가 샌다 → 종료·시간 초과·오류마다 지운다.</li>
 * <li>서버는 일정 시간(30분)이 지나면 연결을 끊는다. 브라우저의 {@code EventSource} 는
 * 자동으로 다시 연결하므로 화면은 그대로 동작한다(heartbeat 없이도 되는 이유).</li>
 * </ul>
 */
@Component
public class NotificationStream {

	/** 서버가 연결을 붙잡고 있는 시간. 지나면 끊고 브라우저가 다시 붙는다. */
	private static final long TIMEOUT_MS = 30L * 60L * 1000L;

	private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

	/**
	 * 구독을 시작한다. 연결되자마자 현재 안 읽은 개수를 한 번 보내 서버와 화면을 맞춘다.
	 *
	 * @param userId      구독하는 사용자 id
	 * @param unreadCount 지금 안 읽은 알림 수
	 */
	public SseEmitter subscribe(Long userId, long unreadCount) {
		SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
		this.emitters.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(emitter);

		emitter.onCompletion(() -> remove(userId, emitter));
		emitter.onTimeout(() -> remove(userId, emitter));
		emitter.onError(error -> remove(userId, emitter));

		send(userId, emitter, "connected", unreadCount);
		return emitter;
	}

	/**
	 * 그 사람의 열린 연결 모두에 바뀐 개수를 보낸다.
	 *
	 * <p>
	 * 연결이 없으면(로그아웃·다른 탭 없음) 아무 일도 하지 않는다. 알림은 DB 에 이미 저장되어 있으므로
	 * 다음에 목록을 열면 그대로 보인다.
	 */
	public void publish(Long userId, long unreadCount) {
		Set<SseEmitter> targets = this.emitters.get(userId);
		if (targets == null || targets.isEmpty()) {
			return;
		}

		for (SseEmitter emitter : targets) {
			send(userId, emitter, "changed", unreadCount);
		}
	}

	/** 지금 열려 있는 연결 수(테스트·모니터링용). */
	public int connectionCount(Long userId) {
		Set<SseEmitter> targets = this.emitters.get(userId);
		return targets == null ? 0 : targets.size();
	}

	private void send(Long userId, SseEmitter emitter, String name, long unreadCount) {
		try {
			emitter.send(
					SseEmitter.event().name(name).data(Map.of("count", unreadCount), MediaType.APPLICATION_JSON));
		}
		catch (IOException | IllegalStateException ex) {
			// 이미 끊어진 연결이다. 목록에서 지운다.
			remove(userId, emitter);
		}
	}

	private void remove(Long userId, SseEmitter emitter) {
		Set<SseEmitter> targets = this.emitters.get(userId);
		if (targets == null) {
			return;
		}

		targets.remove(emitter);
		if (targets.isEmpty()) {
			this.emitters.remove(userId);
		}
	}

}
