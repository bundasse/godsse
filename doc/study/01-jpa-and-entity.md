# 01. JPA와 엔티티 · 리포지토리

> 이 노트를 읽고 나면: `domain`/`repository` 패키지의 코드가 무슨 일을 하는지 읽을 수 있게 됩니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **ORM**(Object-Relational Mapping, 객체-관계 매핑) | 자바 객체와 DB 테이블을 자동으로 연결해 주는 기술 |
| **JPA**(Java Persistence API) | 자바에서 ORM 을 쓰는 **표준 규격**(인터페이스 모음) |
| **Hibernate** | JPA 규격을 실제로 구현한 라이브러리. Spring Boot 에 기본 포함 |
| **엔티티**(Entity) | DB 테이블 하나에 대응하는 자바 클래스. `@Entity` 를 붙인다 |
| **리포지토리**(Repository) | 엔티티를 저장·조회하는 코드. 인터페이스만 선언하면 구현이 자동 생성된다 |
| **트랜잭션**(Transaction) | 여러 DB 작업을 "전부 성공 아니면 전부 취소"로 묶는 단위 |

Vue 에 비유하면 이렇습니다.

| 프론트엔드 | 백엔드(JPA) |
| --- | --- |
| Pinia 의 `defineStore` 로 상태 정의 | `@Entity` 로 테이블 정의 |
| store 의 `actions` | `Repository` 의 `save` / `findBy...` |
| API 응답 타입(interface) | DTO `record` |

## 2. 왜 필요한가

JPA 없이 DB 를 쓰면 **SQL 문자열을 직접 작성**해야 합니다.

```java
// JPA 없이 (참고용 예시 — 이 프로젝트에서는 쓰지 않는다)
String sql = "INSERT INTO users (handle, email, password_hash, nickname) VALUES (?, ?, ?, ?)";
// ... Connection, PreparedStatement, ResultSet 직접 관리
```

이 방식은 테이블 컬럼 이름이 바뀌면 자바 코드 여러 곳이 깨지고, 조회 결과를 객체로 바꾸는 코드를
매번 반복해서 써야 합니다. JPA 는 이 반복을 없애 줍니다.

- 저장: `userRepository.save(user)` → `INSERT` 자동 실행
- 조회: `findByHandle("godsse")` → `SELECT ... WHERE handle = ?` 자동 실행
- 테이블 생성: 엔티티 필드를 보고 `CREATE TABLE` / `ALTER TABLE` 자동 실행
  (`spring.jpa.hibernate.ddl-auto=update` 설정, 개발용)

## 3. 이 프로젝트에서는

| 파일 / 설정 | 역할 |
| --- | --- |
| `backend/pom.xml` | `spring-boot-starter-data-jpa`, `com.h2database:h2` 의존성 |
| `backend/src/main/resources/application.properties` | DB 접속 주소(`spring.datasource.url`), `ddl-auto`, SQL 로그 출력 |
| `backend/src/main/java/com/godsse/backend/domain/` | 엔티티 5종 + `BaseTimeEntity` + enum |
| `backend/src/main/java/com/godsse/backend/repository/` | 리포지토리 인터페이스 5종 |
| `backend/src/test/java/com/godsse/backend/repository/UserRepositoryTest.java` | 리포지토리 동작 테스트 |
| `backend/src/test/resources/application.properties` | 테스트는 메모리 DB 를 쓰도록 덮어쓰기 |

개발용 DB 는 **H2** 입니다. 설치가 필요 없고 파일 하나(`backend/data/godsse.mv.db`)로 동작합니다.
`http://localhost:8080/h2-console` 에 접속하면 표를 직접 눈으로 확인할 수 있습니다.
(JDBC URL 칸에는 `application.properties` 의 `spring.datasource.url` 값을 그대로 넣습니다)

## 4. 예제 코드 — 실제 프로젝트 코드

### 4.1 엔티티

```java
@Entity                       // 이 클래스가 테이블 하나가 된다
@Table(name = "users")        // 테이블 이름 지정 (user 는 DB 예약어라 users 로 둔다)
public class User extends BaseTimeEntity {

	@Id                                              // 기본키
	@GeneratedValue(strategy = GenerationType.IDENTITY)  // DB 가 1,2,3... 자동 부여
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String handle;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	protected User() {           // JPA 가 사용하는 기본 생성자(직접 호출하지 않는다)
	}

	public User(String handle, String email, String passwordHash, String nickname) {
		this.handle = handle;
		this.email = email;
		this.passwordHash = passwordHash;
		this.nickname = nickname;
	}
	// ... getter
}
```

주요 애노테이션 정리:

| 애노테이션 | 뜻 | Vue 비유 |
| --- | --- | --- |
| `@Entity` | 테이블과 연결 | 컴포넌트 등록 |
| `@Id` + `@GeneratedValue` | 기본키 자동 생성 | — |
| `@Column(nullable = false, length = 20)` | 컬럼 조건 | 폼 검증 규칙 |
| `@ManyToOne` | 다른 엔티티 한 개를 참조(FK) | 부모 객체 참조 |
| `@Enumerated(EnumType.STRING)` | enum 을 이름 문자열로 저장 | — |
| `@PrePersist` / `@PreUpdate` | 저장·수정 직전 자동 실행 | 라이프사이클 훅 |

### 4.2 공통 상위 클래스

```java
@MappedSuperclass          // 테이블이 되지 않고, 자식에게 컬럼만 물려준다 (mixIn 같은 역할)
public abstract class BaseTimeEntity {

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}
}
```

### 4.3 리포지토리 — 인터페이스만 선언하면 구현이 생긴다

```java
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByHandle(String handle);      // SELECT ... WHERE handle = ?
	boolean existsByHandle(String handle);           // SELECT COUNT(*) ... (중복 확인)
	Page<User> findByHandleContainingIgnoreCaseOrNicknameContainingIgnoreCase(
			String handle, String nickname, Pageable pageable);  // 검색 + 페이지
}
```

메서드 이름이 곧 쿼리입니다. 규칙을 외우기보다 "이름을 읽으면 무슨 쿼리인지 보인다" 정도로 이해하면 됩니다.

| 이름 조각 | 의미 |
| --- | --- |
| `findBy` / `existsBy` / `countBy` | 조회 / 존재 여부 / 개수 |
| `...And...` / `...Or...` | 조건 결합 |
| `ContainingIgnoreCase` | 부분 일치 + 대소문자 무시(`LIKE %값%`) |
| `OrderBy...Desc` | 정렬 |
| `FollowerId` | 연관 객체 `follower` 의 `id` 를 조건으로 (`follower.id`) |

### 4.4 테스트

```java
@SpringBootTest
@Transactional      // 테스트가 끝나면 변경 내용을 롤백한다 → DB 가 더러워지지 않는다
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("User 를 저장하면 handle 로 조회할 수 있다")
	void saveAndFindByHandle() {
		User saved = this.userRepository.save(new User("godsse", "godsse@example.com", "hash", "고드세"));

		assertThat(saved.getId()).isNotNull();
		assertThat(this.userRepository.findByHandle("godsse")).isPresent();
	}
}
```

## 5. 꼭 알아 둘 함정: 지연 로딩(LAZY)과 `open-in-view=false`

`Review` 는 `author`(User) 를 `@ManyToOne(fetch = FetchType.LAZY)` 로 참조합니다. LAZY 는
"꺼내올 때가 되면 그때 조회한다"는 뜻입니다.

이때 **트랜잭션이 끝난 뒤에 `review.getAuthor().getNickname()` 을 호출하면 오류**가 납니다
(`LazyInitializationException`). 그래서 이 프로젝트는 다음 규칙을 지킵니다.

> **규칙**: DB 에서 꺼낸 엔티티는 **서비스의 `@Transactional` 메서드 안에서** 화면용 DTO 로 바꾼 뒤
> 컨트롤러로 넘긴다. 컨트롤러와 화면은 엔티티를 직접 만지지 않는다.

`application.properties` 의 `spring.jpa.open-in-view=false` 는 이 문제를 **숨기지 않고 빨리 드러나게**
하기 위한 설정입니다(기본값은 `true` 라서 문제가 가려진다).

## 6. 확인 문제

1. `User` 클래스에서 `@Column(unique = true)` 가 붙은 필드는 무엇인가요?
2. `FollowRepository.findByFollowerIdAndFollowingId(...)` 는 어떤 SQL 이 실행될까요?
3. `Review.body` 를 화면에 보여 주려면 어느 시점(트랜잭션 안/밖)에 DTO 로 바꿔야 할까요?
4. `Page<User>` 를 반환하는 메서드에 `Pageable` 인자가 있는 이유는 무엇일까요?

## 관련 문서

- 도메인/테이블 상세 → [../07-domain-model.md](../07-domain-model.md)
- 백엔드 구조 → [../03-backend.md](../03-backend.md)
