# 07. 도메인 모델 (데이터 구조)

이 문서는 godsse 가 다루는 데이터와 그 관계를 정리합니다. 코드 기준 위치는
`backend/src/main/java/com/godsse/backend/domain` 입니다.

- 최종 갱신: 2026-09-24

## 1. 설계 방향 (A안)

| 결정 | 내용 | 이유 |
| --- | --- | --- |
| 세션 정보 위치 | **리뷰(`Review`)가 직접 가진다** | 날짜·룰·시나리오를 리뷰가 그대로 들고 있으면 스키마와 화면이 단순하다 |
| 공유 세션 테이블 | **두지 않는다** | 30~50명 테스트 규모에서는 "같은 세션"을 묶는 복잡도가 이득보다 크다 |
| 달력 | 별도 `SessionPlan` | 예정된 일정은 나만 보는 계획표라서 리뷰와 성격이 다르다 |

> 나중에 "같은 세션에 참가한 여러 사람의 리뷰를 하나로 묶고 싶다"는 요구가 생기면, `Session` 테이블을
> 추가하고 `Review.session_id`(nullable)를 붙이는 방식으로 확장합니다. 그때까지는 만들지 않습니다.

## 2. 관계도

```
        ┌───────────────┐
        │     users     │
        └───┬───────┬───┘
   author_id│       │recipient_id
        ┌───▼───────▼───┐        ┌────────────────┐
        │    reviews    │        │  session_plans │
        └───────┬───────┘        └───────┬────────┘
                │                        │owner_id
        ┌───────▼────────┐       ┌───────▼────────┐
        │  review_tags   │       │     users      │
        └────────────────┘       └────────────────┘

        users ──(follower_id)──► follows ──(following_id)──► users
```

| 엔티티 | 테이블 | 설명 |
| --- | --- | --- |
| `User` | `users` | 사용자. `handle`(고유 아이디) + `nickname`(화면 이름) |
| `Follow` | `follows` | 팔로우 관계 |
| `Review` | `reviews` | 감상 메시지(리뷰). 세션 정보를 포함 |
| `RuleSystem` | `rule_systems` | 룰 자동완성용 목록 |
| `SessionPlan` | `session_plans` | 마이페이지 달력의 세션 일정 |
| (값 목록) | `review_tags` | 리뷰의 감정 태그(`@ElementCollection`) |

## 3. 테이블 상세

### 3.1 `users`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, 자동 증가 | |
| `handle` | VARCHAR(20) | NOT NULL, UNIQUE | 고유 핸들. `/u/{handle}` URL 과 검색에 사용 |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE | 로그인 아이디 |
| `password_hash` | VARCHAR(100) | NOT NULL | BCrypt 해시(원문 저장 안 함) |
| `nickname` | VARCHAR(20) | NOT NULL | 화면 표시 이름(중복 허용) |
| `bio` | VARCHAR(300) | NULL | 자기소개 |
| `profile_image_url` | VARCHAR(300) | NULL | 예: `/uploads/profiles/xxxx.png` |
| `created_at` | TIMESTAMP | NOT NULL | `@PrePersist` 로 기록 |
| `updated_at` | TIMESTAMP | NOT NULL | `@PrePersist` / `@PreUpdate` |

### 3.2 `reviews`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `author_id` | BIGINT | NOT NULL, FK→users | 리뷰를 쓴 사람 |
| `recipient_id` | BIGINT | NOT NULL, FK→users | 리뷰를 받는 사람 |
| `played_on` | DATE | NOT NULL | 플레이 날짜 (**필수 입력**) |
| `rule_name` | VARCHAR(50) | NOT NULL | 룰 (**필수 입력**) |
| `scenario_name` | VARCHAR(100) | NULL | 시나리오명 (**선택 입력**) |
| `body` | VARCHAR(10000) | NOT NULL | 감상 본문. 100자 이상은 요청 DTO 에서 검증 |
| `spoiler_mode` | VARCHAR(20) | NOT NULL | `NONE` / `BLUR` / `FOLD` |
| `visibility` | VARCHAR(20) | NOT NULL | `PUBLIC` / `FOLLOWERS` / `PRIVATE` |
| `created_at`, `updated_at` | TIMESTAMP | NOT NULL | |

인덱스: `idx_reviews_recipient(recipient_id)`, `idx_reviews_author(author_id)`

### 3.3 `review_tags`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `review_id` | BIGINT | NOT NULL, FK→reviews | |
| `tag_code` | VARCHAR(30) | NOT NULL | 예: `RP`, `BATTLE`, `HORROR` |

### 3.4 `follows`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `follower_id` | BIGINT | NOT NULL, FK→users | 팔로우를 거는 사람 |
| `following_id` | BIGINT | NOT NULL, FK→users | 팔로우를 받는 사람 |

제약: `uk_follows_pair (follower_id, following_id)` — 같은 조합 중복 저장 방지

### 3.5 `session_plans`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `owner_id` | BIGINT | NOT NULL, FK→users | 일정 소유자 |
| `title` | VARCHAR(60) | NOT NULL | 일정 제목 |
| `planned_date` | DATE | NOT NULL | 예정일 |
| `rule_name` | VARCHAR(50) | NOT NULL | 룰 |
| `scenario_name` | VARCHAR(100) | NULL | 시나리오명 |
| `gm` | BOOLEAN | NOT NULL | 내가 GM 인지 |
| `memo` | VARCHAR(1000) | NULL | 메모 |
| `status` | VARCHAR(20) | NOT NULL | `PLANNED` / `DONE` / `CANCELED` |

인덱스: `idx_session_plans_owner_date(owner_id, planned_date)`

### 3.6 `rule_systems`

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | |
| `code` | VARCHAR(30) | NOT NULL, UNIQUE | 예: `COC7` |
| `label` | VARCHAR(60) | NOT NULL | 예: `크툴루의 부름 7판` |
| `sort_order` | INT | NOT NULL | 목록 정렬 |
| `active` | BOOLEAN | NOT NULL | 사용 여부 |

> 리뷰는 FK 가 아니라 `rule_name` 문자열을 저장합니다. 초기에는 자유 입력을 허용하고, 데이터가 쌓이면
> 정규화(코드 참조)를 검토합니다.

## 4. 스포일러 규칙

스포일러는 **두 단계**로 다룹니다.

| 단계 | 저장 방식 | 화면 처리 |
| --- | --- | --- |
| 리뷰 전체 | `spoiler_mode` 컬럼 (`NONE`/`BLUR`/`FOLD`) | 카드·상세에서 본문 전체를 가리거나 접는다 |
| 본문 일부 | 본문에 마커를 **문자 그대로** 저장 | 화면에서 마커를 해석해 그 부분만 가린다 |

마커 규칙:

| 마커 | 위치 | 뜻 |
| --- | --- | --- |
| `\|\|문장\|\|` | 문장 안 | 그 문장을 흐리게 가리고 클릭하면 보여 준다 |
| `:::spoiler` | 단독 줄 | 그 지점 이후 전체를 접는다 |

```text
오늘 정말 즐거웠습니다. ||범인은 집사였습니다.|| 다음에도 또 해요!
:::spoiler
여기서부터는 결말 이야기입니다.
```

- **마커를 문자로 남기는 이유**: 위치(인덱스)를 따로 저장하면 작성자가 글을 고칠 때마다 다시 계산해야 하고
  계산이 틀리면 엉뚱한 문장이 가려진다. 마커는 글과 함께 움직이므로 안전하다.
- **짝이 맞지 않는 마커**(예: `||` 하나만 있는 경우)는 자르지 않고 원문 그대로 보여 준다. 데이터를 잃지 않기 위함이다.

## 5. 관련 문서

- 백엔드 구조 → [03-backend.md](./03-backend.md)
- API 명세 → [05-api.md](./05-api.md)
- JPA 개념 학습 노트 → [study/01-jpa-and-entity.md](./study/01-jpa-and-entity.md)
