# 08. 좋아요·답장 설계 (관계 테이블 · 카운트 성능)

> 이 노트를 읽고 나면: 좋아요 같은 "가벼운 반응"을 어떻게 저장하는지,
> 목록에서 개수·여부를 보여 줄 때 무엇을 조심해야 하는지 설명할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **관계 테이블** | 두 대상의 관계 한 줄을 저장하는 테이블(`review_likes`) |
| **1:N 자식 테이블** | 부모 하나에 여러 행이 붙는 구조(`review_replies`) |
| **유니크 제약** | 같은 조합을 두 번 저장하지 못하게 하는 DB 규칙 |
| **집계(count)** | 조건에 맞는 행의 개수. 목록에서 "하트 3" 같은 숫자 |
| **N+1** | 목록 1번 + 항목마다 1번씩 = N+1 번 쿼리가 나가는 문제 |

## 2. 왜 이렇게 나누는가

| 기능 | 성격 | 저장 방식 | 이유 |
| --- | --- | --- | --- |
| **좋아요** | "내가 눌렀다/안 눌렀다" 두 상태 | 관계 테이블 + (리뷰, 사용자) 유니크 | 중복을 막고, "내가 눌렀는지"를 한 행으로 확인 |
| **답장** | 여러 개의 글 | 1:N 자식 테이블 | 개수 제한 없이 쌓이고, 순서(오래된 순)가 의미 있음 |

좋아요를 `reviews` 테이블의 컬럼(`liked_by`)으로 만들 수는 없습니다.
여러 명이 누르는 순간 "여러 값"이 필요하기 때문입니다. 그래서 **관계를 행으로** 저장합니다.

```
reviews                 review_likes                users
┌────┐                 ┌────┬───────────┬─────────┐  ┌────┐
│ id │                 │ id │ review_id │ user_id │  │ id │
├────┤                 ├────┼───────────┼─────────┤  ├────┤
│ 65 │◀────────────────│  1 │    65     │    12   │─▶│ 12 │
└────┘                 └────┴───────────┴─────────┘  └────┘
                         uk(review_id, user_id) ← 같은 사람이 두 번 누를 수 없음
```

## 3. 이 프로젝트에서는

| 파일 | 역할 |
| --- | --- |
| `domain/ReviewLike.java` | 좋아요 한 건(리뷰 + 사용자, 유니크) |
| `domain/ReviewReply.java` | 답장 한 건(리뷰 + 작성자 + 본문 500자) |
| `repository/ReviewLikeRepository.java` | 존재 확인·개수·**내가 누른 id 목록** |
| `repository/ReviewReplyRepository.java` | 목록(오래된 순)·개수 |
| `service/ReviewService.java` | 좋아요/취소/답장 + 응답에 개수·여부 매핑 |
| `frontend/components/LikeButton.jsx` | 하트 토글 |
| `frontend/components/ReviewReplies.jsx` | 답장 목록·작성·삭제 |

### 3.1 중복 좋아요를 막는 두 겹

```java
@Transactional
public void like(User loginUser, Long reviewId) {
	Review review = getReview(reviewId);
	requireVisible(review, loginUser);          // 볼 수 없는 감상이면 403

	if (this.reviewLikeRepository.existsByReviewIdAndUserId(reviewId, loginUser.getId())) {
		throw new ConflictException("이미 좋아요한 감상입니다.");   // 409
	}

	this.reviewLikeRepository.save(new ReviewLike(review, getUserById(loginUser.getId())));
}
```

| 겹 | 역할 |
| --- | --- |
| 서비스의 `existsBy...` | 사용자에게 **409 + 친절한 메시지**를 주기 위해 |
| DB 의 `uk_review_likes` | 동시에 두 번 눌러도 **데이터가 깨지지 않도록** 마지막에 막아 준다 |

### 3.2 목록에서 "내가 눌렀는지" — N+1 피하기

카드 20개에 대해 각각 "내가 눌렀나?" 를 물으면 20번 쿼리가 나갑니다.
그래서 **페이지에 담긴 id 를 한 번에 넘겨** 누른 것만 받아 옵니다.

```java
@Query("select l.review.id from ReviewLike l where l.user.id = :userId and l.review.id in :reviewIds")
List<Long> findLikedReviewIds(@Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);
```

```java
private Page<ReviewSummary> toSummaryPage(Page<Review> page, User viewer) {
	Set<Long> likedIds = likedReviewIds(page.getContent(), viewer);   // 쿼리 1회

	return page.map(review -> ReviewSummary.of(review,
			this.reviewLikeRepository.countByReviewId(review.getId()),   // 항목마다 1회 (아래 설명)
			likedIds.contains(review.getId())));
}
```

### 3.3 절충한 부분 — 좋아요 "개수"는 항목마다 센다

지금은 카드마다 `countByReviewId` 를 실행합니다(20건이면 20회). 규모가 작아 문제가 없고,
코드가 읽기 쉽기 때문입니다. 목록이 커지면 다음 중 하나로 바꿉니다.

| 개선안 | 방법 |
| --- | --- |
| 묶어서 세기 | `select l.review.id, count(l) ... group by l.review.id` 한 번으로 가져와 Map 으로 |
| 카운터 컬럼 | `reviews.like_count` 를 두고 좋아요/취소 때 `+1 / -1` (조회는 컬럼만 읽음) |
| 캐시 | 인기 글의 개수를 캐시에 두기 |

> **"언제 최적화할지"도 설계의 일부**입니다. 지금 규모(30~50명)에서는 단순함이 더 큰 가치입니다.

## 4. 확인 문제

1. 좋아요를 `users` 테이블의 컬럼으로 만들 수 없는 이유는 무엇일까요?
2. 좋아요 중복을 서비스와 DB **둘 다**에서 막는 이유는 무엇일까요?
3. 목록에서 "내가 좋아요했는지"를 항목마다 조회하면 어떤 문제가 생길까요?
4. 좋아요 개수를 항목마다 세는 방식의 장점과 단점은 무엇일까요?

## 관련 문서

- API 명세(좋아요·답장) → [../05-api.md](../05-api.md)
- 도메인 모델(`review_likes`, `review_replies`) → [../07-domain-model.md](../07-domain-model.md)
- 감상 설계와 스포일러 → [06-review-spoiler.md](./06-review-spoiler.md)
