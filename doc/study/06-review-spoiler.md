# 06. 감상(리뷰) 설계와 스포일러 처리

> 이 노트를 읽고 나면: 감상이 어떻게 저장·조회되는지, 스포일러가 왜 "마커를 그대로 저장"하는 방식인지,
> 목록에서 어떻게 스포일러를 흘리지 않는지 설명할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **스포일러 마커** | 본문 안에 넣는 표시. 화면에서 이 표시를 찾아 가리거나 접는다 |
| **BLUR(가리기)** | `\|\|문장\|\|` — 그 문장만 흐리게 가리고, 누르면 보인다 |
| **FOLD(접기)** | 단독 줄 `:::spoiler` — 그 지점 이후를 접는다 |
| **미리보기(preview)** | 목록 카드에 넣을 짧은 본문. 여기서 스포일러가 새면 안 된다 |
| **공개 범위(visibility)** | PUBLIC / FOLLOWERS / PRIVATE |
| **가시성 필터** | "조회자가 볼 수 있는 것"만 골라내는 조건 |

## 2. 왜 스포일러를 "마커 그대로" 저장하는가

두 가지 방법을 생각할 수 있습니다.

| 방법 | 내용 | 문제 |
| --- | --- | --- |
| 위치(인덱스) 저장 | `spoilerStart = 152` 처럼 숫자로 저장 | 작성자가 글을 고치면 위치를 다시 계산해야 하고, 계산이 틀리면 **엉뚱한 문장이 가려지거나 그대로 노출**된다 |
| **마커를 문자로 저장(채택)** | `\|\|범인은 집사\|\|` 를 글자 그대로 저장 | 글과 함께 움직이므로 수정해도 안전. 화면에서만 해석한다 |

그래서 백엔드는 마커를 **해석하지 않습니다**. 저장·조회 모두 원문 그대로입니다.

```
작성 화면 ──▶ DB(원문 그대로 저장) ──▶ 화면(SpoilerText 가 해석해 가림)
```

## 3. 이 프로젝트에서는

| 파일 | 역할 |
| --- | --- |
| `domain/Review.java` | 감상 한 건. 날짜·룰·시나리오·본문·스포일러 모드·공개 범위·태그 |
| `service/ReviewService.java` | 작성·조회·수정·삭제, 공개 범위 판단(`canView`) |
| `repository/ReviewRepository.java` | 목록 쿼리(공개 범위 조건 포함), JPQL |
| `common/SpoilerMarkers.java` | 마커 문자열 상수(프론트와 같은 값) |
| `common/ReviewPreview.java` | 목록용 미리보기 만들기(스포일러 안전 절단) |
| `frontend/components/SpoilerText.jsx` | 화면에서 마커 해석 |
| `frontend/constants/spoilerMarkers.js` | 프론트의 마커·모드·공개 범위 정의 |

### 3.1 필수값 검증은 DTO 에서

```java
public record ReviewCreateRequest(
		@NotBlank String toHandle,
		@NotNull @PastOrPresent LocalDate playedOn,      // 플레이 날짜(필수, 미래 불가)
		@NotBlank @Size(max = 50) String ruleName,       // 룰(필수)
		@Size(max = 100) String scenarioName,            // 시나리오(선택)
		@NotBlank @Size(min = 100, max = 10000) String body,   // 본문(100자 이상)
		SpoilerMode spoilerMode,                         // 없으면 NONE
		Visibility visibility,                           // 없으면 PUBLIC
		@Size(max = 8) Set<String> tags) {
}
```

`spoilerMode`·`visibility` 는 생략할 수 있게 두고, 서비스에서 기본값을 채웁니다.
(요청이 간단해지고, 기본 동작이 분명해진다)

### 3.2 목록 미리보기 — 자르다가 스포일러를 흘리지 않기

그냥 앞에서 200자만 자르면 위험합니다.
`||` 짝이 잘려 나가면 화면은 "짝이 안 맞는 마커"로 보고 **원문 그대로 보여 주기** 때문입니다.

```java
public static String of(String body, int limit) {
	String text = body.strip();
	if (text.length() <= limit) return text;

	int end = limit;

	// 접기 표시가 앞에 있으면 그 앞까지만 (뒤는 스포일러일 수 있다)
	int foldIndex = text.indexOf(SpoilerMarkers.FOLD);
	if (foldIndex >= 0 && foldIndex < end) end = foldIndex;

	String cut = text.substring(0, end);

	// 가리기 표시의 짝이 잘렸으면 그 지점부터 버린다
	if (count(cut, SpoilerMarkers.BLUR) % 2 != 0) {
		cut = cut.substring(0, cut.lastIndexOf(SpoilerMarkers.BLUR));
	}

	return cut.stripTrailing() + "…";
}
```

### 3.3 목록에서도 공개 범위를 지킨다 (JPQL)

목록은 "가져온 뒤 걸러내기"가 아니라 **쿼리에서 걸러야** 합니다. 그래야 페이지 수(total)도 맞습니다.

```java
@Query("""
		select r from Review r
		where r.recipient.id = :userId
		  and (r.visibility = :publicVisibility                     -- 누구나
		       or r.author.id = :viewerId                          -- 작성자 본인
		       or r.recipient.id = :viewerId                       -- 받은 사람
		       or (r.visibility = :followersVisibility             -- 팔로워 공개
		           and exists (select 1 from Follow f
		                       where f.follower.id = :viewerId and f.following.id = r.author.id)))
		order by r.playedOn desc, r.createdAt desc
		""")
Page<Review> findReceivedVisibleTo(...);
```

> 비로그인이면 `:viewerId` 에 **null** 을 넘깁니다.
> SQL 에서 `r.author.id = null` 은 참이 될 수 없으므로 로그인/비로그인을 같은 쿼리로 처리할 수 있습니다.

단건 조회는 엔티티를 읽은 뒤 자바 코드로 판단합니다(`canView`) — 조건이 여러 개라 쿼리보다 읽기 쉽습니다.

```java
private boolean canView(Review review, User viewer) {
	if (review.getVisibility() == Visibility.PUBLIC) return true;
	if (viewer == null) return false;
	if (isAuthor(review, viewer) || review.getRecipient().getId().equals(viewer.getId())) return true;
	if (review.getVisibility() == Visibility.FOLLOWERS) {
		return this.followRepository.existsByFollowerIdAndFollowingId(viewer.getId(), review.getAuthor().getId());
	}
	return false;
}
```

### 3.4 화면에서 해석하기 (SpoilerText)

```jsx
// || 로 나눈 조각들 — 홀수 번째가 가려진 부분
function InlineText({ text, interactive }) {
  const parts = splitByBlur(text)
  if (!parts) return text            // 짝이 안 맞으면 원문 그대로
  return parts.map((part, index) =>
    index % 2 === 0 ? <span key={index}>{part}</span>
                    : <BlurPiece key={index} interactive={interactive} text={part} />)
}
```

카드(목록)에서는 `interactive={false}` 로 씁니다. 카드 전체가 링크라서, 안에서 또 클릭 가능한
요소를 두면 "가리기를 눌렀는데 상세 페이지로 이동"하는 문제가 생기기 때문입니다.

## 4. 확인 문제

1. 스포일러 위치를 인덱스로 저장하면 어떤 사고가 날 수 있을까요?
2. 목록 미리보기를 그냥 `substring(0, 200)` 하면 왜 위험할까요?
3. 목록에서 공개 범위를 "가져온 뒤 자바로 거르기" 하면 어떤 값이 틀어질까요?
4. `:viewerId` 에 null 을 넣어도 되는 이유는 무엇일까요?

## 관련 문서

- API 명세(감상) → [../05-api.md](../05-api.md)
- 도메인 모델(`reviews` 테이블) → [../07-domain-model.md](../07-domain-model.md)
- 팔로우와 JPQL → [05-follow-and-queries.md](./05-follow-and-queries.md)
