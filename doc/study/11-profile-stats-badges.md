# 11. 집계(count)와 "계산해서 보여 주는 값" — 프로필 통계·배지

> 이 노트를 읽고 나면: 개수를 세는 방법을 상황에 맞게 고르고, **저장할 것과 계산할 것**을
> 나누는 기준을 잡을 수 있습니다.

## 1. 개수를 세는 세 가지 방법

| 방법 | 예 | 비용 | 언제 |
| --- | --- | --- | --- |
| 목록을 읽어 `size()` | `repository.findAll().size()` | **행 전체를 메모리로** 올린다 | 거의 쓰지 않는다 |
| count 쿼리 | `countByAuthorId(id)` | DB 가 세어서 숫자만 준다 | 기본값 |
| 카운터 컬럼 | `users.review_count` 를 +1/-1 | 조회는 빠르지만 **갱신 누락**에 취약 | 조회가 매우 많고 즉시성이 필요할 때 |

```java
// 숫자만 필요하면 count 쿼리. (감상 1000편이어도 결과는 1행)
long writtenReviewCount = reviewRepository.countByAuthorId(user.getId());
```

## 2. "이 사람이 쓴 감상들이 받은 하트"는 어떻게 셀까

감상마다 `countByReviewId()` 를 부르면 **감상 수만큼 쿼리**가 나갑니다(N+1).

```java
// ❌ 감상 100편이면 쿼리 100번
for (Review review : reviews) { total += likeRepository.countByReviewId(review.getId()); }

// ✅ 한 번
@Query("select count(l) from ReviewLike l where l.review.author.id = :authorId")
long countByReviewAuthorId(@Param("authorId") Long authorId);
```

관계를 따라가는 조건(`l.review.author.id`)은 SQL 로 번역될 때 **조인**이 됩니다.
그래서 "감상을 먼저 읽고 → 좋아요를 세는" 2단계가 "조인해서 한 번에" 로 줄어듭니다.

## 3. 파생 쿼리(derived query) 읽는 법

메서드 이름만으로 쿼리를 만들어 주는 기능입니다. **엔티티의 필드 이름을 이어 붙여** 해석합니다.

```java
// ReviewReply.review → Review.author → User.id
long countByReviewAuthorId(Long authorId);
//   count  By  Review    Author    Id
```

| 이름 | 실제 조건 |
| --- | --- |
| `countByReviewId` | `review.id = ?` |
| `countByReviewAuthorId` | `review.author.id = ?` |
| `existsByFollowerIdAndFollowingId` | `follower.id = ? and following.id = ?` |

조건이 길어지거나(`or`, 서브쿼리) `select` 할 대상이 다르면 `@Query` 로 직접 씁니다.

## 4. 배지: 저장할까, 계산할까?

| | 저장(테이블) | **계산(이 프로젝트)** |
| --- | --- | --- |
| 조회 | 빠름 | count 몇 번 더 |
| 규칙 변경 | 기존 사용자 배지 **재계산 필요** | 코드만 고치면 끝 |
| 취소 반영 | 좋아요 취소 시 회수 로직 필요 | 자동(숫자가 줄면 사라짐) |
| 구현량 | 테이블·이벤트·배치 | 순수 함수 하나 |

> "지금 숫자에서 바로 나오는 값"은 저장하지 않는 편이 단순합니다.
> 대신 **조회 비용**이 늘어나므로, 배지가 수십 개가 되면 그때 캐시를 검토합니다.

```java
public static List<String> of(long writtenReviewCount, long receivedReviewCount,
		long receivedLikeCount, long followerCount) {
	List<String> badges = new ArrayList<>();
	if (writtenReviewCount >= 1) { badges.add(FIRST_REVIEW); }
	...
}
```

계산 규칙은 **DB 가 필요 없는 순수 함수**라 테스트도 빠릅니다(`ProfileBadgesTest` — 스프링 없이 실행).

## 5. 서버는 코드, 화면은 문구

| 계층 | 책임 |
| --- | --- |
| 서버 | `["FIRST_REVIEW", "HEARTS_10"]` — **조건 판단**만 |
| 화면 | 🖊️ 첫 감상 / ❤️ 하트 10개 — **아이콘·문구** |

- 문구를 고치려고 서버를 다시 배포하지 않아도 됩니다.
- **모르는 코드는 그리지 않습니다**(`PROFILE_BADGE_MAP.get(code)` → 없으면 `null`).
  서버가 먼저 배포되어도 화면이 깨지지 않는 "앞뒤 호환" 습관입니다.

## 6. 함정

| 함정 | 대응 |
| --- | --- |
| 목록 `size()` 로 개수 세기 | count 쿼리 사용 |
| 감상마다 count 호출(N+1) | 조인 조건으로 한 번에 |
| 배지를 저장하고 회수 로직을 잊음 | 계산으로 처리 |
| 공개 범위 무시 | 통계는 **전체 수**(공개 범위와 무관)라고 문서에 명시 |
| 서버에 한국어 문구 하드코딩 | 코드/문구 분리 |

## 7. 확인 문제

1. `countByReviewAuthorId` 는 어떤 필드를 따라가나요?
2. 좋아요 취소로 하트가 9개가 되면 `HEARTS_10` 배지는 어떻게 될까요? (계산/저장 각각)
3. 프로필 조회에서 쿼리는 몇 번 나갈까요? (핸들 조회 + count 6 + 팔로우 여부 1)

## 관련 문서

- API 명세(프로필 응답·배지 코드) → [../05-api.md](../05-api.md)
- 실시간 알림 → [10-realtime-sse.md](./10-realtime-sse.md)
** 여기까지 읽음 **