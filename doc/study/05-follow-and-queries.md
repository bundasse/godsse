# 05. 팔로우 관계와 JPQL 조회

> 이 노트를 읽고 나면: "누가 누구를 팔로우한다"를 테이블로 어떻게 표현하는지,
> 목록 조회에서 N+1 문제를 어떻게 피하는지 설명할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **N:M(다대다) 관계** | 여러 명이 여러 명과 맺는 관계(팔로우, 좋아요 등) |
| **연결(중간) 테이블** | 다대다를 "1:N 두 개"로 풀어내는 테이블. 여기서는 `follows` |
| **자기 참조** | 한 테이블이 자기 자신과 관계를 맺는 것(`users` → `users`) |
| **유니크 제약** | 같은 값 조합을 두 번 저장하지 못하게 하는 DB 규칙 |
| **JPQL** | 테이블이 아니라 **엔티티**를 대상으로 쓰는 쿼리 언어 |
| **N+1 문제** | 목록 1번 + 각 항목마다 1번씩 = N+1 번 쿼리가 나가는 상황 |

Vue 비유: `users` 는 사용자 목록, `follows` 는 "A가 B를 팔로우함"이라는 관계 목록입니다.
프론트에서 `follows.filter(f => f.followerId === me)` 로 목록을 만드는 일을 DB 가 대신한다고 보면 됩니다.

## 2. 왜 연결 테이블이 필요한가

`users` 테이블에 팔로우 목록을 컬럼으로 넣을 수는 없습니다(여러 명을 한 칸에 담을 수 없음).
그래서 관계 자체를 한 줄씩 저장합니다.

```
users                      follows                          users
┌────┬─────────┐           ┌────┬─────────────┬─────────────┐  ┌────┬─────────┐
│ id │ handle  │           │ id │ follower_id │ following_id│  │ id │ handle  │
├────┼─────────┤           ├────┼─────────────┼─────────────┤  ├────┼─────────┤
│  1 │ a6447   │◀──────────│  1 │      1      │      2      │─▶│  2 │ b340    │
└────┴─────────┘           └────┴─────────────┴─────────────┘  └────┴─────────┘
                             "1번이 2번을 팔로우한다"
```

규칙 두 가지를 꼭 지켜야 합니다.

| 규칙 | 이유 | 우리 코드 |
| --- | --- | --- |
| 같은 조합은 한 번만 | 중복 저장 시 팔로워 수가 부풀고 언팔로우가 애매해짐 | DB `uk_follows_pair` + 서비스에서 `existsBy...` 로 409 |
| 자기 자신은 불가 | "나를 팔로우"는 의미가 없음 | 서비스에서 `me.getId().equals(target.getId())` → 400 |

## 3. 이 프로젝트에서는

| 파일 | 역할 |
| --- | --- |
| `domain/Follow.java` | `follower`(거는 사람) / `following`(받는 사람) 두 개의 `@ManyToOne` |
| `repository/FollowRepository.java` | 존재 확인·개수 세기 + **JPQL 목록 조회** |
| `service/FollowService.java` | 팔로우/언팔로우/목록, 자기 자신·중복 검사 |
| `api/FollowController.java` | `POST/DELETE /api/users/{handle}/follow`, `GET .../followers`·`.../following` |
| `api/dto/UserProfileDetailResponse.java` | `followerCount`, `followingCount`, `isFollowing` |

### 3.1 엔티티 — 같은 테이블을 두 번 참조

```java
@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(name = "uk_follows_pair",
		columnNames = { "follower_id", "following_id" }))
public class Follow extends BaseTimeEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "follower_id", nullable = false)   // 거는 사람
	private User follower;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "following_id", nullable = false)  // 받는 사람
	private User following;
}
```

### 3.2 목록 조회 — N+1 을 피하는 JPQL

`Follow` 행을 먼저 가져와 `getFollower()` 를 읽으면, 행마다 사용자를 다시 조회합니다.

```java
// ❌ Follow 를 페이지로 가져오고 → 각 행의 follower 를 읽을 때마다 SELECT (N+1)
Page<Follow> rows = followRepository.findByFollowingId(id, pageable);
rows.map(row -> UserSummary.of(row.getFollower()));   // 20건이면 20번 더 조회
```

처음부터 필요한 엔티티를 고르면 쿼리 한 번으로 끝납니다.

```java
// ✅ JPQL: Follow 를 거치지 않고 User 를 바로 선택
@Query("select f.follower from Follow f where f.following.id = :userId order by f.createdAt desc")
Page<User> findFollowerUsers(@Param("userId") Long userId, Pageable pageable);
```

> ⚠️ **정렬 주의**: `@Query` 를 쓸 때 `Pageable` 에 `Sort` 를 담아 넘기면 Spring Data 가 JPQL 의
> 별칭을 해석하지 못해 오류가 납니다. 정렬은 위처럼 **쿼리 안에** 적고, `Pageable` 에는
> 페이지 정보만 담습니다.

### 3.3 자기 자신·중복 검사

```java
@Transactional
public void follow(User loginUser, String targetHandle) {
	User me = getUserById(loginUser.getId());        // 트랜잭션 밖 객체일 수 있어 다시 조회
	User target = getUserByHandle(targetHandle);

	if (me.getId().equals(target.getId())) {
		throw new BadRequestException("handle", "자기 자신은 팔로우할 수 없습니다.");   // 400
	}
	if (this.followRepository.existsByFollowerIdAndFollowingId(me.getId(), target.getId())) {
		throw new ConflictException("handle", "이미 팔로우한 사용자입니다.");            // 409
	}

	this.followRepository.save(new Follow(me, target));
}
```

### 3.4 "내가 팔로우 중인지"를 프로필에 담기

프로필 조회는 로그인 없이도 가능하므로 조회자를 **선택**으로 받습니다.

```java
@GetMapping("/{handle}")
public UserProfileDetailResponse profile(@PathVariable String handle,
		@LoginUser(required = false) User viewer) {   // 비로그인이면 null
	return this.userService.getProfile(handle, viewer);
}
```

```java
boolean isFollowing = viewer != null
		&& this.followRepository.existsByFollowerIdAndFollowingId(viewer.getId(), user.getId());
```

`@LoginUser(required = false)` 로 두면 "로그인했으면 알려 주고, 아니면 null" 이 되어
공개 API 와 로그인 전용 기능을 한 엔드포인트에서 함께 다룰 수 있습니다.

## 4. 확인 문제

1. 팔로우 목록을 `users` 테이블 컬럼으로 만들 수 없는 이유는 무엇일까요?
2. 같은 사람을 두 번 팔로우하지 못하게 막는 방법 두 가지(DB·서비스)는 무엇이며, 왜 둘 다 둘까요?
3. `Follow` 목록을 그대로 읽는 방식이 N+1 이 되는 이유는 무엇일까요?
4. `@Query` 와 `Pageable` 의 `Sort` 를 함께 쓰면 왜 오류가 날까요?

## 관련 문서

- 도메인 모델(`follows` 테이블) → [../07-domain-model.md](../07-domain-model.md)
- API 명세(팔로우) → [../05-api.md](../05-api.md)
- JPA 와 엔티티 → [01-jpa-and-entity.md](./01-jpa-and-entity.md)
