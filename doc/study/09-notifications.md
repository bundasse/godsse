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

## 7. 알림 종류를 늘릴 때 — 감상 요청(`REVIEW_REQUEST`)

"세션을 마쳤으니 감상을 남겨 달라"고 **일정 소유자가 함께한 사람에게** 보내는 알림을 추가했습니다.

- 만드는 곳: `SessionPlanService.requestReviews` (요청 API 는 소유자만 호출)
- 받는 사람: 그 일정의 **참가자**
- **이미 나에게 감상을 쓴 사람은 뺍니다** — 이미 쓴 사람에게 "써 주세요"는 알림이 아니라 잔소리입니다.
  응답의 `requestedCount` 는 "실제로 보낸 사람 수"라, 0 이면 화면이 "모두 남겼습니다"로 안내할 수 있습니다.
- **아직 읽지 않은 요청이 있으면 다시 보내지 않습니다** — 요청 기록을 저장하지 않으므로, 그냥 두면
  버튼을 다섯 번 누른 만큼 같은 알림이 다섯 줄 쌓입니다. 반대로 "한 번 보냈으면 영원히 금지"하면
  나중에 독촉할 방법이 없습니다. 그래서 기준을 **안 읽음(pending)** 에 두었습니다.

```java
// NotificationService — 정책은 알림 쪽에, 호출은 감상 요청 쪽에
@Transactional
public boolean notifyUnlessPending(User recipient, User actor, NotificationType type, Review review) {
	if (this.notificationRepository.existsByRecipientIdAndActorIdAndTypeAndReadAtIsNull(
			recipient.getId(), actor.getId(), type)) {
		return false;
	}
	notify(recipient, actor, type, review);
	return true;
}
```

> 왜 `notify` 안에 넣지 않았을까? 좋아요·답장은 **매번 새 사실**이라 중복 제거가 오히려 정보를 잃습니다
> (같은 감상에 두 번 답장하면 알림도 두 번이 맞습니다). 중복이 문제인 것은 "같은 부탁을 반복"하는
> 감상 요청뿐이라, 규칙을 그 호출에만 붙입니다.
- 관련 감상이 없으므로 `reviewId` 는 `null` 입니다. 그래서 화면의 이동 규칙이 종류마다 달라집니다.

```js
// NotificationsPage.jsx — 종류별로 갈 곳이 다르다
if (type === 'FOLLOW') return `/u/${actor.handle}`        // 상대 프로필
if (type === 'REVIEW_REQUEST') return `/reviews/new?to=${actor.handle}`  // 상대에게 쓰는 화면
return `/reviews/${reviewId}`                              // 나머지는 관련 감상
```

> **왜 "그 일정으로" 가지 않을까?** 세션 일정은 **나만 보는 계획표**입니다(소유자만 조회 가능).
> 요청받은 사람이 그 일정을 열 수는 없으니, 알림은 "누구에게 쓰면 되는지"까지만 알려 주고
> 감상 작성 화면으로 보냅니다. 알림 한 줄에 **다른 도메인의 화면으로 보낼 수 있는 최소 정보**(`actor`)만 담는 이유입니다.

### 함정: enum 타입 컬럼

`NotificationType` 에 `REVIEW_REQUEST` 를 추가했더니 **테스트는 통과하는데 실서버에서만 500** 이 났습니다.

- 테스트는 `create-drop` 이라 스키마를 새로 만들고, 그때는 `REVIEW_REQUEST` 도 포함된 `ENUM(...)` 이 만들어집니다.
- 개발 DB(파일 H2)는 **예전에 만들어진 컬럼**을 그대로 씁니다. Hibernate 의 `ddl-auto=update` 는
  **이미 있는 컬럼의 타입(값 목록)을 바꾸지 않습니다.** 그래서 새 값만 저장이 거부됩니다.

해결: 알림 종류처럼 **값이 계속 늘어나는 컬럼**은 DB enum 에 맡기지 않고 문자열로 고정합니다.

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
private NotificationType type;
```

이미 만들어진 DB 는 한 번만 바꿔 주면 됩니다.
`ALTER TABLE notifications ALTER COLUMN type SET DATA TYPE VARCHAR(20);`

## 8. 확인 문제

1. 알림 테이블에서 `read_at` 대신 `is_read` boolean 을 써도 될까요? 무엇이 달라질까요?
2. 좋아요 취소 시 알림을 지우지 않으면 어떤 문제가 생길까요?
3. 답장 알림을 감상 작성자에게만 보내는 것과, 받은 사람에게도 보내는 것의 차이는 무엇일까요?
4. 헤더 배지를 30초마다 폴링하는 방식과 화면 이동 때 조회하는 방식의 장단점은?
5. 감상 요청 알림에서 **이미 감상을 쓴 사람을 빼는** 판단은 어디에 두는 게 좋을까요? (컨트롤러/서비스/DB)
6. enum 값을 하나 추가했을 때 "테스트는 통과하는데 실서버에서만 실패"하는 상황을 미리 잡으려면 무엇을 확인해야 할까요?

## 관련 문서

- API 명세(알림) → [../05-api.md](../05-api.md)
- 도메인 모델(`notifications`) → [../07-domain-model.md](../07-domain-model.md)
- 좋아요·답장 설계 → [08-like-and-reply.md](./08-like-and-reply.md)
** 여기까지 읽음 **