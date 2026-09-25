# 03. 세션 로그인 (HttpSession · 쿠키 · BCrypt)

> 이 노트를 읽고 나면: 로그인하면 왜 그다음 요청부터 "내 정보"가 보이는지,
> 비밀번호가 DB 에 어떻게 저장되는지 설명할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **HTTP** | 요청-응답이 끝나면 서버가 클라이언트를 잊어버리는 **무상태(stateless)** 규약 |
| **세션(Session)** | 서버가 "로그인한 사람"을 기억해 두는 저장 공간 |
| **쿠키(Cookie)** | 브라우저가 서버 대신 보관하는 작은 값. 서버는 여기에 **세션 번호**만 담아 보낸다 |
| **JSESSIONID** | Tomcat 이 붙이는 세션 쿠키 이름 |
| **BCrypt** | 비밀번호를 되돌릴 수 없는 값(해시)으로 바꾸는 알고리즘 |
| **해시(hash)** | 원문 → 고정 길이 값. 같은 입력은 같은 결과, 결과에서 원문 복원 불가 |
| **세션 고정 공격** | 공격자가 미리 아는 세션 ID 로 로그인시켜 그 ID 를 훔쳐 보는 수법 |
| **HttpOnly / SameSite** | 쿠키 보안 속성. JS 로 읽기 금지 / 다른 사이트발 요청에 쿠키 전송 제한 |

## 2. 왜 필요한가

HTTP 는 상태가 없어서, 서버는 두 번째 요청이 "아까 그 사람"인지 알 수 없습니다.
해결 방법은 크게 두 가지입니다.

| 방식 | 저장 위치 | 특징 |
| --- | --- | --- |
| **세션 + 쿠키(이 프로젝트)** | 로그인 정보: 서버 메모리 / 브라우저: 세션 번호만 | 서버가 로그아웃을 즉시 강제할 수 있다. 토큰을 JS 가 들고 있지 않아 XSS 에 강함 |
| **JWT 토큰** | 브라우저(localStorage 등) | 서버가 상태를 안 가져도 됨(확장 쉬움). 대신 토큰 무효화가 어렵고 저장 위치 관리가 필요 |

**30~50명 규모의 웹사이트**에서는 세션이 단순하고 안전합니다. (모바일 앱을 만들 때 JWT 를 다시 검토)

## 3. 이 프로젝트에서는

### 3.1 로그인 흐름

```
[브라우저]                      [Spring Boot]
  POST /api/auth/login  ──────▶  AuthController.login()
  {handle, password}              └─ AuthService.login()
                                      ├─ UserRepository.findByHandle()
                                      ├─ passwordEncoder.matches(입력, 저장된해시)
                                      └─ 틀리면 UnauthorizedException → 401
  ◀── 200 + Set-Cookie: JSESSIONID=ABC123  (세션에 LOGIN_USER_ID 저장)
  GET /api/auth/me      ──────▶  (브라우저가 쿠키를 자동 전송)
  Cookie: JSESSIONID=ABC123
                                  └─ LoginUserArgumentResolver
                                       ├─ 세션에서 LOGIN_USER_ID 꺼냄
                                       ├─ UserRepository.findById()
                                       └─ me(@LoginUser User user) 파라미터로 전달
  ◀── 200 { handle, nickname, ... }
```

### 3.2 관련 파일

| 파일 | 역할 |
| --- | --- |
| `api/AuthController.java` | signup/login/logout/me, 세션 시작·종료 |
| `service/AuthService.java` | 비밀번호 해시·검증, 중복 확인 |
| `config/PasswordConfig.java` | `BCryptPasswordEncoder` 빈 등록 |
| `common/SessionKeys.java` | 세션에 담는 키(`LOGIN_USER_ID`) |
| `common/LoginUser.java` | `@LoginUser` 애노테이션 |
| `common/LoginUserArgumentResolver.java` | 세션 → 사용자 객체로 바꿔 주입 |
| `config/WebConfig.java` | 위 resolver 등록 |
| `application.properties` | 세션 쿠키(HttpOnly, SameSite), 유효 시간 |
| `frontend/src/context/AuthProvider.jsx` | 앱 시작 시 `/api/auth/me` 로 로그인 복원 |
| `frontend/src/api/client.js` | `credentials: 'include'`, 401 → `ApiError` |

## 4. 예제 코드

### 4.1 세션에는 "사용자 번호"만 담는다

```java
// common/SessionKeys.java
public static final String LOGIN_USER_ID = "LOGIN_USER_ID";

// api/AuthController.java
private void startSession(HttpServletRequest request, Long userId) {
	request.getSession(true);
	request.changeSessionId();   // 세션 고정 공격 방지: 로그인 시 ID 를 새로 발급
	request.getSession().setAttribute(SessionKeys.LOGIN_USER_ID, userId);
}
```

닉네임이나 프로필 이미지까지 세션에 담아 두면, 사용자가 프로필을 바꿨을 때 세션과 DB 가 어긋납니다.
**세션은 "누구인지(id)"만, 나머지는 DB 가 기준**입니다.

### 4.2 비밀번호는 저장하지 않고 "비교"만 한다

```java
// 저장할 때
String passwordHash = this.passwordEncoder.encode(request.password());  // 예: $2a$10$...

// 로그인할 때
if (!this.passwordEncoder.matches(request.password(), user.getPasswordHash())) {
	throw new UnauthorizedException("핸들 또는 비밀번호가 올바르지 않습니다.");
}
```

`encode()` 는 매번 다른 결과가 나옵니다(소금값 때문). 그래서 저장된 값과 문자열 비교를 하면 안 되고,
반드시 `matches()` 를 씁니다.

### 4.3 세션 → 컨트롤러 파라미터

```java
@GetMapping("/me")
public UserProfileResponse me(@LoginUser User user) {   // 로그인 안 했으면 여기서 401
	return UserProfileResponse.of(user);
}
```

`LoginUserArgumentResolver` 가 파라미터를 채우지 못하면 `UnauthorizedException` 을 던지고,
`ApiExceptionHandler` 가 401 JSON 으로 바꿔 줍니다.

### 4.4 쿠키 보안 설정

```properties
server.servlet.session.cookie.http-only=true    # JS 에서 쿠키 못 읽음 (XSS 완화)
server.servlet.session.cookie.same-site=lax     # 다른 사이트발 요청엔 쿠키 미전송 (CSRF 완화)
server.servlet.session.timeout=30m              # 30분 미사용 시 로그아웃
```

### 4.5 프론트엔드에서 쿠키 주고받기

```js
// frontend/src/api/client.js
fetch(`${BASE_URL}${path}`, {
  credentials: 'include',   // 쿠키(JSESSIONID)를 함께 보내고 받는다
  headers: { 'Content-Type': 'application/json' },
})
```

로그인 성공/실패는 눈에 보이는 "토큰"이 아니라 **쿠키 + 서버 상태**로 결정되므로,
프론트는 `sessionStorage` 같은 곳에 아무것도 저장하지 않습니다.

## 5. 지금 방식의 한계 (알아 두기)

| 한계 | 대비 |
| --- | --- |
| 세션이 서버 **메모리**에 있어 서버를 재시작하면 로그아웃됨 | 개발 중에는 자연스러운 동작. 배포 시에는 Redis 같은 외부 저장소(spring-session)로 이전 |
| 서버를 여러 대로 늘리면 세션 공유가 안 됨 | 위와 같은 이유로 외부 세션 저장소 필요 |
| CSRF(다른 사이트에서 우리 API 를 호출) | SameSite=Lax 로 완화. 불특정 다수 개방(Phase 4) 전에 Spring Security 의 CSRF 토큰 도입 검토 |

## 6. 확인 문제

1. 브라우저 쿠키에 담기는 것은 "로그인 정보"일까요, "세션 번호"일까요?
2. `changeSessionId()` 를 로그인 시점에 호출하는 이유는 무엇일까요?
3. DB 에 저장된 해시만 보고 원래 비밀번호를 알 수 있을까요? 왜 그럴까요?
4. 로그인 상태를 세션에 두면 프론트엔드는 왜 토큰을 저장할 필요가 없을까요?

## 관련 문서

- API 명세(인증) → [../05-api.md](../05-api.md)
- 계층 구조와 트랜잭션 → [02-spring-layers.md](./02-spring-layers.md)
- JPA 와 엔티티 → [01-jpa-and-entity.md](./01-jpa-and-entity.md)
