# 09. 알림 설계 (곁다리 기능을 안전하게 붙이기)

> 이 노트를 읽고 나면: 알림을 "이벤트 기록"으로 설계하는 이유와,
> 기존 기능에 알림을 붙일 때 **무엇을 만들지 말아야 하는지** 설명할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **알림(notification)** | "누가(actor) 누구에게(recipient) 무슨 일(type)이 있었다"는 기록 한 줄 |
| **읽음 시각(`read_at`)** | NULL 이면 안 읽음. 값이 있으면 읽은 시각 |
| **배지(badge)** | 헤더에 붙는 안 읽은 개수 표시 |
| **곁다리 기능(cross-cutting)** | 감상·좋아요·팔로우 등 여러 기능에 걸쳐 만들어지는 기능 |

알림은 **사용자가 직접 만드는 데이터가 아닙니다.** 다른 행동의 결과로만 생깁니다.
그래서 "알림을 어떻게 저장할까"보다 **"언제 만들고, 언제 만들지 않을까"** 가 설계의 중심입니다.

## 2. 테이블은 "이벤트 한 줄"로

```
notifications
┌────┬──────────────┬──────────┬────────┬───────────┬─────────┐
│ id │ recipient_id │ actor_id │  type  │ review_id │ read_at │
├────┼──────────────┼──────────┼────────┼───────────┼─────────┤
│  1 │   (받은 사람) │ (행동자) │  LIKE  │    65     │  NULL   │ ← 안 읽음
│  2 │   (받은 사람) │ (행동자) │ FOLLOW │   NULL    │ 10:20   │ ← 읽음
└────┴──────────────┴──────────┴────────┴───────────┴─────────┘
```

| 선택 | 대안 | 왜 이렇게 했나 |
| --- | --- | --- |
| 한 테이블에 `type` | 종류별 테이블(`like_notifications` …) | 종류가 늘어도 테이블을 안 바꾼다 |
| `review_id` 는 **NULL 허용** | 모든 알림에 감상 필수 | 팔로우 알림은 감상이 없다 |
| `read_at` 시각 | `is_read` boolean | "언제 읽었는지"까지 남고, boolean 과 정보량이 같다 |

## 3. 만들지 말아야 할 때 (중요)

```java
@Transactional
public void notify(User recipient, User actor, NotificationType type, Review review) {
	if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
		return;   // ① 내 활동은 나를 깨우지 않는다
	}
	this.notificationRepository.save(new Notification(recipient, actor, type, review));
}
```

| 규칙 | 이유 | 예 |
| --- | --- | --- |
| ① 자기 행동 제외 | 내가 한 일로 알림이 쌓이면 목록이 쓸모없어진다 | 내 감상에 내가 좋아요 |
| ② **되돌린 행동은 알림도 삭제** | "좋아요를 눌렀습니다"가 남았는데 취소했다면 거짓말이 된다 | 좋아요 취소, 언팔로우 |
| ③ 받는 사람이 여럿이면 각각 (본인 제외) | 답장은 작성자·받은 사람 모두 궁금하다 | 감상 작성자 + 받은 사람 |

```java
// 좋아요 취소 — 되돌렸으니 알림도 지운다
this.notificationRepository.deleteByTypeAndReviewIdAndActorId(NotificationType.LIKE, review.getId(), actor.getId());
```

> ②를 하지 않으면 알림 목록이 **과거의 거짓**을 담게 됩니다. 지우는 편이 조회 로직을 단순하게 유지합니다.

## 4. 어디에 붙이는가 — 서비스 직접 호출 vs 이벤트

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| **서비스에서 직접 호출**(이 프로젝트) | 읽기 쉽고, 트랜잭션 하나로 묶여 **행동과 알림이 함께 성공/실패** | 서비스끼리 의존이 늘어난다 |
| 스프링 이벤트(`@TransactionalEventListener`) | 결합이 느슨하다 | 흐름이 눈에 안 보이고, 실패 처리가 어려워진다 |

규모가 작고 규칙이 단순할 때는 **직접 호출**이 낫습니다.
"좋아요 저장은 됐는데 알림은 안 생겼다" 같은 어긋남이 원천적으로 없기 때문입니다.

## 5. 헤더 배지는 어떻게 최신으로 유지할까

| 방법 | 특징 |
| --- | --- |
| **화면 이동 때마다 조회**(이 프로젝트) | 구현이 가장 단순. 알림 화면에서 읽고 돌아오면 배지가 사라진다 |
| 주기적 폴링(30초 등) | 새 알림을 비교적 빨리 안다. 쿼리가 계속 나간다 |
| SSE / WebSocket | 실시간. 서버·클라이언트 구조가 복잡해진다 |

```jsx
useEffect(() => {
  if (!user) return undefined
  fetchUnreadCount().then((data) => setCount(data.count))
  // 화면(pathname)이 바뀔 때마다 다시 센다
}, [user, location.pathname])
```

> 처음엔 "화면 이동 때 조회"로 충분합니다. 실시간성이 필요해지면 그때 SSE 를 검토합니다.

## 6. 읽음 처리에서 주의할 점

- 목록에서 **모두 읽음**은 한 번의 `UPDATE` 로 처리합니다(행을 하나씩 읽어 고치지 않는다).

```java
@Modifying(clearAutomatically = true)
@Query("update Notification n set n.readAt = :now where n.recipient.id = :recipientId and n.readAt is null")
int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);
```

- 남의 알림 id 로 읽음 처리하면 **403 이 아니라 404** 를 줍니다. (남의 알림이 존재하는지조차 알려 주지 않는다)

## 7. 확인 문제

1. 알림 테이블에서 `read_at` 대신 `is_read` boolean 을 써도 될까요? 무엇이 달라질까요?
2. 좋아요 취소 시 알림을 지우지 않으면 어떤 문제가 생길까요?
3. 답장 알림을 감상 작성자에게만 보내는 것과, 받은 사람에게도 보내는 것의 차이는 무엇일까요?
4. 헤더 배지를 30초마다 폴링하는 방식과 화면 이동 때 조회하는 방식의 장단점은?

## 관련 문서

- API 명세(알림) → [../05-api.md](../05-api.md)
- 도메인 모델(`notifications`) → [../07-domain-model.md](../07-domain-model.md)
- 좋아요·답장 설계 → [08-like-and-reply.md](./08-like-and-reply.md)
