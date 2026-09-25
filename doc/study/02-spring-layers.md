# 02. 계층 구조(Controller–Service–Repository)와 트랜잭션

> 이 노트를 읽고 나면: `api` / `service` / `repository` 가 왜 나뉘어 있는지,
> `@Transactional` 이 무슨 일을 하는지 이해할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **계층(layer)** | 역할별로 코드를 나눈 층. 요청 처리 · 로직 · 저장을 각각 맡는다 |
| **Controller** | HTTP 요청을 받아 검증하고 응답을 돌려주는 담당(창구) |
| **Service** | 실제 규칙·계산·판단을 하는 담당(일꾼). 트랜잭션 경계가 여기에 있다 |
| **Repository** | DB 에 넣고 꺼내는 담당(창고지기) |
| **트랜잭션(transaction)** | 여러 DB 작업을 "전부 성공 아니면 전부 취소"로 묶는 단위 |
| **ArgumentResolver** | 컨트롤러 파라미터를 자동으로 채워 주는 장치 (`@LoginUser`) |

Vue 와 비교하면:

| 프론트엔드 | 백엔드 |
| --- | --- |
| 컴포넌트(화면) — 클릭/입력 처리 | Controller — HTTP 요청/응답 |
| Pinia action(로직) | Service — 비즈니스 로직 |
| axios/fetch 함수 | Repository — 저장소 접근 |
| try/catch 로 오류 표시 | 예외 던지기 → `ApiExceptionHandler` 가 JSON 변환 |

## 2. 왜 필요한가

한 파일에서 요청 받기 + 규칙 판단 + SQL 까지 다 하면:

- 같은 규칙을 다른 API 에서 재사용하기 어렵다 (회원가입/프로필 수정 모두 핸들 중복을 확인해야 함)
- 테스트하기 어렵다 (HTTP 없이는 로직을 실행할 수 없음)
- 트랜잭션 범위가 애매해져 **일부만 저장되는 사고**가 난다

계층을 나누면 "규칙은 `service` 한곳에만" 존재하게 되고, 테스트도 쉬워집니다.

## 3. 이 프로젝트에서는

```
HTTP 요청
  └─ api/AuthController          (@Valid 검증, 세션 저장, 응답 코드)
       └─ service/AuthService    (@Transactional, 중복 확인, BCrypt, 예외 던지기)
            └─ repository/UserRepository   (Spring Data JPA)
                 └─ domain/User            (테이블)
```

| 파일 | 하는 일 |
| --- | --- |
| `api/AuthController.java` | URL 매핑, 세션 시작/종료, `201/204` 응답 코드 |
| `api/dto/SignupRequest.java` | 입력 형식 검증(`@Pattern`, `@Email`, `@Size`) |
| `service/AuthService.java` | 핸들·이메일 중복 확인, 비밀번호 해시, 실패 시 예외 |
| `service/UserService.java` | 사용자 조회(`getByHandle`, `findById`) |
| `repository/UserRepository.java` | `existsByHandle` 등 DB 접근 |
| `common/exception/*` | 서비스가 던지는 예외(404/409/401) |
| `api/ApiExceptionHandler.java` | 던져진 예외를 `{ message, errors[] }` JSON 으로 변환 |
| `common/LoginUserArgumentResolver.java` | `@LoginUser User user` 파라미터를 세션 + DB 로 채움 |

## 4. 예제 코드

### 4.1 Controller — 창구 역할만 한다

```java
@PostMapping("/signup")
@ResponseStatus(HttpStatus.CREATED)
public UserProfileResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
	UserProfileResponse profile = this.authService.signup(request);  // 로직은 서비스에
	startSession(httpRequest, profile.id());                          // 세션은 HTTP 관심사라 여기서
	return profile;
}
```

### 4.2 Service — 규칙과 트랜잭션

```java
@Service
@Transactional(readOnly = true)   // 이 클래스의 기본은 읽기 전용
public class AuthService {

	@Transactional                // 값을 바꾸는 메서드만 쓰기 트랜잭션
	public UserProfileResponse signup(SignupRequest request) {
		String handle = normalizeHandle(request.handle());

		if (this.userRepository.existsByHandle(handle)) {
			throw new ConflictException("handle", "이미 사용 중인 핸들입니다.");   // → 409
		}

		String passwordHash = this.passwordEncoder.encode(request.password());
		User saved = this.userRepository.save(new User(handle, email, passwordHash, nickname));
		return UserProfileResponse.of(saved);   // 엔티티를 그대로 내보내지 않는다
	}
}
```

`@Transactional` 이 붙은 메서드는 Spring 이 **프록시(대리 객체)** 로 감싸서 실행합니다.

```
컨트롤러 → [프록시] 트랜잭션 시작 → 실제 signup() 실행 → 정상 종료면 commit, 예외면 rollback
```

그래서 **같은 클래스 안에서 자기 메서드를 호출하면 프록시를 거치지 않아 트랜잭션이 걸리지 않습니다.**
(예: `signup()` 안에서 `this.helper()` 를 부르면 `helper()` 의 `@Transactional` 은 무시됨)

### 4.3 Repository — 인터페이스만 (자동 구현)

```java
public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByHandle(String handle);
	Optional<User> findByHandle(String handle);
}
```

### 4.4 예외 → 응답 변환

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ValidationErrorResponse> handleBusiness(BusinessException exception) {
	List<FieldValidationError> errors = exception.getField() == null ? List.of()
			: List.of(new FieldValidationError(exception.getField(), exception.getMessage()));

	return ResponseEntity.status(exception.getStatus())
		.body(new ValidationErrorResponse(exception.getMessage(), errors));
}
```

서비스는 "왜 실패했는지"만 던지고, "몇 번 코드로 내려갈지"는 예외 클래스가 정합니다.
(`ConflictException` → 409, `NotFoundException` → 404, `UnauthorizedException` → 401)

## 5. 확인 문제

1. `@Valid` 검증 실패는 어느 계층에서 걸러질까요? (Controller / Service / Repository)
2. 핸들 중복 확인 코드는 왜 컨트롤러가 아니라 `AuthService` 에 있을까요?
3. `AuthService.signup()` 이 실패해서 예외가 던져지면 이미 저장된 데이터는 어떻게 될까요?
4. 같은 클래스 안에서 `this.다른메서드()` 를 호출하면 `@Transactional` 이 왜 안 걸릴까요?

## 관련 문서

- API 명세 → [../05-api.md](../05-api.md)
- 백엔드 구조 → [../03-backend.md](../03-backend.md)
- 세션 로그인 → [03-session-login.md](./03-session-login.md)
