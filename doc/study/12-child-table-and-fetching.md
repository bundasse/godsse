# 12. 자식 테이블(1:N)과 조회 전략 — 세션 참가자

> 이 노트를 읽고 나면: "값 목록"과 "자식 테이블"을 구분해 고르고,
> 1:N 목록을 N+1 없이 읽고, 삭제 순서(FK)와 일괄 삭제의 함정을 피할 수 있습니다.

## 1. 1:N 을 저장하는 두 가지 방법

| 방법 | 예 | 특징 |
| --- | --- | --- |
| **값 목록**(`@ElementCollection`) | `review_tags` (문자열 태그) | 행이 다른 테이블을 참조하지 않는다. 수명이 부모와 같다 |
| **자식 엔티티**(`@ManyToOne`) | `session_participants` (사용자 참조) | 다른 테이블을 참조하고, **그 자체로 조회·확장**할 수 있다 |

> 판단 기준: "이 값이 **다른 엔티티를 가리키는가**?"
> 태그는 문자열이라 값 목록, 참가자는 `users` 를 가리키므로 자식 테이블입니다.

```java
@Entity
@Table(name = "session_participants",
		uniqueConstraints = @UniqueConstraint(name = "uk_session_participants",
				columnNames = { "session_plan_id", "user_id" }))
public class SessionParticipant extends BaseTimeEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_plan_id", nullable = false)
	private SessionPlan plan;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
```

부모 쪽에 `List<SessionParticipant>` 를 **두지 않았습니다**(단방향).
자식이 부모를 참조하기만 하면 되고, 목록은 리포지토리로 직접 읽는 편이 단순합니다.

## 2. 달력 한 달치를 그릴 때 생기는 N+1

```java
// ❌ 일정 10건이면 참가자 조회 10번
for (SessionPlan plan : plans) {
	plan.setParticipants(repository.findByPlanId(plan.getId()));
}
```

```java
// ✅ id 목록으로 한 번에 가져와 일정별로 묶는다
List<Long> planIds = plans.stream().map(SessionPlan::getId).toList();
Map<Long, List<UserSummary>> byPlan = new HashMap<>();

for (SessionParticipant participant : participantRepository.findByPlanIdsWithUser(planIds)) {
	byPlan.computeIfAbsent(participant.getPlan().getId(), key -> new ArrayList<>())
		.add(UserSummary.of(participant.getUser()));
}
```

| 방법 | 언제 |
| --- | --- |
| `join fetch` | 자식과 부모(또는 참조 엔티티)를 **함께** 읽어야 할 때 |
| 일괄 조회 + `Map` 묶기 | 목록 화면처럼 **여러 부모**의 자식이 필요할 때 |
| `@EntityGraph` | 파생 쿼리(`findBy...`)에 fetch 를 붙이고 싶을 때 |

`join fetch` 는 `sp.user` 처럼 **참조 엔티티의 지연 로딩 반복**도 함께 막아 줍니다.
(참가자 20명의 닉네임을 꺼낼 때 쿼리 20번이 나가는 것을 막음)

## 3. 지연 로딩은 "트랜잭션 안에서" 꺼내야 한다

`@ManyToOne(fetch = LAZY)` 는 **프록시**만 들고 있다가, 실제 값을 쓸 때 쿼리를 보냅니다.
그래서 서비스(트랜잭션) 안에서 DTO 로 바꿔 두어야 화면 계층에서 터지지 않습니다.

```java
// 서비스 안: 트랜잭션이 살아 있으므로 user 를 꺼낼 수 있다
.map(participant -> UserSummary.of(participant.getUser()))
```

이 프로젝트는 `spring.jpa.open-in-view` 기본값(켜짐)에 기대지 않고,
**서비스에서 DTO 로 변환**하는 규칙을 지킵니다.

## 4. 삭제 순서: 자식 → 부모

FK 제약 때문에 부모를 먼저 지우면 실패합니다.

```java
@Transactional
public void delete(User loginUser, Long id) {
	SessionPlan plan = getOwnedPlan(loginUser, id);

	participantRepository.deleteByPlanId(plan.getId());   // 자식 먼저
	sessionPlanRepository.delete(plan);                   // 그다음 부모
}
```

## 5. ⚠️ 일괄 삭제(bulk delete)의 함정 — 실제로 겪은 버그

처음에는 이렇게 썼습니다.

```java
@Modifying
@Query("delete from SessionParticipant sp where sp.plan.id = :planId")
void deleteByPlanId(@Param("planId") Long planId);
```

테스트에서 이런 오류가 났습니다.

```text
TransientPropertyValueException: Persistent instance of 'SessionParticipant'
references an unsaved transient instance of 'SessionPlan'
```

| 왜? | 설명 |
| --- | --- |
| JPQL `delete` 는 **영속성 컨텍스트를 거치지 않는다** | DB 행만 지우고, 1차 캐시의 엔티티는 그대로 남는다 |
| 남은 엔티티가 flush 대상이 된다 | 부모를 `remove` 한 뒤 flush 하면서 "자식이 사라진 부모를 가리킨다"고 판단 |
| 해결 | `@Modifying(clearAutomatically = true, flushAutomatically = true)` |
| 더 단순한 해결 | **파생 삭제**(`void deleteByPlanId(Long planId)`) — 엔티티를 읽어 정상적으로 remove |

> 성능이 정말 중요한 대량 삭제가 아니라면, **파생 삭제**가 안전합니다.
> bulk 연산을 쓸 때는 `clearAutomatically`/`flushAutomatically` 를 세트로 기억하세요.

## 6. 유니크 제약과 서비스 검증을 둘 다 두는 이유

```java
if (existsByPlanIdAndUserId(plan.getId(), participant.getId())) {
	throw new ConflictException("이미 참가자로 기록된 사람입니다.");   // 409 — 사용자에게 설명
}
```

| 계층 | 역할 |
| --- | --- |
| 서비스 검증 | **친절한 409 메시지** |
| DB 유니크 제약 | 동시 요청·버그에 대한 **마지막 안전장치** |

## 7. 오류를 상황별로 나누기

| 상황 | 코드 | 이유 |
| --- | --- | --- |
| `handle` 이 빈 값, 나 자신 | 400 | 요청이 잘못됨 |
| 없는 핸들·없는 일정 | 404 | 대상이 없음 |
| 이미 참가자 | 409 | 현재 상태와 충돌 |
| 남의 일정 | 403 | 권한 없음 |

같은 "실패"라도 **원인이 다르면 코드가 달라야** 화면에서 안내 문구를 고를 수 있습니다.

## 8. 확인 문제

1. `review_tags` 는 값 목록, `session_participants` 는 자식 테이블인 이유는?
2. 달력 한 달치에서 N+1 을 없애는 두 가지 방법은?
3. JPQL `delete` 를 쓸 때 `clearAutomatically` 를 빼면 어떤 문제가 생길까요?
4. 유니크 제약이 있는데도 서비스에서 중복을 확인하는 이유는?

## 관련 문서

- API 명세(참가자) → [../05-api.md](../05-api.md)
- 도메인 모델(`session_participants`) → [../07-domain-model.md](../07-domain-model.md)
- 집계와 계산된 값 → [11-profile-stats-badges.md](./11-profile-stats-badges.md)
** 여기까지 읽음 **
아이고 하기싫다