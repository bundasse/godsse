# 10. 실시간 알림 (SSE · Server-Sent Events)

> 이 노트를 읽고 나면: 실시간을 얻는 세 가지 방법을 비교하고,
> SSE 서버를 **메모리 누수 없이** 만들고, `EventSource` 로 받아 화면을 갱신할 수 있습니다.

## 1. 실시간을 얻는 세 가지 방법

| 방법 | 흐름 | 장점 | 단점 |
| --- | --- | --- | --- |
| **폴링(polling)** | 클라이언트가 주기적으로 "새 거 있어?" | 가장 단순 | 쿼리가 계속 나가고, 늦게 안다 |
| **SSE** | 한 번 연결 → 서버가 필요할 때 밀어 줌 (한 방향) | 표준 HTTP·자동 재연결·구현 쉬움 | 클라 → 서버는 못 보냄(필요 없으면 충분) |
| **WebSocket** | 양방향 통로 | 양방향·저지연 | 서버·프록시·인증 설정이 복잡 |

알림은 **서버 → 화면 한 방향**이면 충분하므로 SSE 를 고릅니다.
(브라우저가 `EventSource` 로 자동 재연결까지 해 준다)

## 2. 응답을 "끝내지 않는" 요청

```text
GET /api/notifications/stream
Content-Type: text/event-stream

event:connected
data:{"count":0}

event:changed
data:{"count":1}
```

- 요청 하나가 **끝나지 않고 열려 있는 채로** 여러 이벤트를 흘려보냅니다.
- 이벤트 형식은 `event:`(이름), `data:`(내용), 그리고 빈 줄(구분자)입니다.

## 3. 서버: 등록 → 전송 → 정리

```java
public SseEmitter subscribe(Long userId, long unreadCount) {
	SseEmitter emitter = new SseEmitter(TIMEOUT_MS);                       // 30분
	this.emitters.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(emitter);

	emitter.onCompletion(() -> remove(userId, emitter));                   // 정상 종료
	emitter.onTimeout(() -> remove(userId, emitter));                      // 시간 초과
	emitter.onError(error -> remove(userId, emitter));                     // 오류

	send(userId, emitter, "connected", unreadCount);                      // 연결 즉시 한 번
	return emitter;
}
```

| 포인트 | 이유 |
| --- | --- |
| `ConcurrentHashMap` | 연결은 **요청 스레드가 끝난 뒤 다른 스레드**에서도 전송된다 |
| **세 가지 콜백에서 제거** | 안 지우면 끊긴 emitter 가 계속 쌓여 **메모리가 샌다** |
| 타임아웃 30분 | 너무 길면 죽은 연결이 오래 남고, 너무 짧으면 재연결이 잦다 |
| 연결 직후 `connected` 1회 | 탭을 연 시점의 개수와 서버를 **먼저 맞춘다** |

전송은 항상 실패 가능성을 감싼다.

```java
try {
	emitter.send(SseEmitter.event().name(name).data(Map.of("count", unreadCount), MediaType.APPLICATION_JSON));
} catch (IOException | IllegalStateException ex) {
	remove(userId, emitter);    // 이미 끊어진 연결
}
```

## 4. "신호만 보내고, 내용은 DB 에서"

이 프로젝트의 `changed` 이벤트는 **개수 하나**만 담습니다.

| 방식 | 문제 |
| --- | --- |
| 이벤트에 알림 전문을 담기 | 서버·클라이언트가 **같은 모델을 두 번** 관리한다. 읽음/삭제와 어긋나기 쉽다 |
| **개수만 보내고 목록은 API 로** | 원본은 DB 하나. 놓친 이벤트가 있어도 화면을 열면 정확하다 |

> 실시간 스트림은 **"다시 읽어라"는 신호**로 쓰는 편이 오래 간다.

## 5. 어디서 밀어 주는가

| 시점 | 누구에게 | 이유 |
| --- | --- | --- |
| 알림 생성 | 받는 사람 | 새 알림 |
| 좋아요 취소·언팔로우 | 받는 사람 | 알림이 **지워졌으니** 개수도 줄어야 한다 |
| 하나/모두 읽음 | **나** | 다른 탭·창의 배지도 같이 사라져야 한다 |

## 6. 함정 정리

| 함정 | 이 프로젝트의 대응 |
| --- | --- |
| 끊긴 연결 정리 | 종료·시간 초과·오류·전송 실패 시 목록에서 제거 |
| 프록시가 응답을 모음 | 로컬 개발은 문제 없음. 운영에서는 heartbeat(주석 줄)나 `X-Accel-Buffering: no` 검토 |
| **커밋 전에 전송** | 서비스 트랜잭션 안에서 save → count 조회(자동 flush) 후 전송. 롤백되면 화면이 한 번 어긋날 수 있어, `@TransactionalEventListener(AFTER_COMMIT)` 로 옮기는 것이 다음 개선점 |
| 세션 만료 | 재연결 시 401 → `EventSource` 는 재시도만 반복하므로, 화면에서는 로그아웃 처리 필요 |
| 여러 탭 | 탭마다 연결 1개. `Set<SseEmitter>` 라서 모두에게 전송된다 |
| 테스트 | MockMvc 의 `request().asyncStarted()` + 응답 본문에 `event:` 문자열 확인, 끝나면 `MockAsyncContext.complete()` |

## 7. 확인 문제

1. 알림에 SSE 를 고르고 WebSocket 을 고르지 않은 이유는 무엇일까요?
2. `onCompletion`/`onTimeout`/`onError` 중 하나라도 빠지면 무슨 일이 생길까요?
3. 이벤트에 알림 전문을 담지 않고 **개수만** 보내는 이유는 무엇일까요?
4. 좋아요 **취소** 때도 push 해야 하는 이유는 무엇일까요?

## 관련 문서

- API 명세(알림·SSE) → [../05-api.md](../05-api.md)
- 알림 설계 → [09-notifications.md](./09-notifications.md)
