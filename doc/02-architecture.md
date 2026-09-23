# 02. 아키텍처와 요청 흐름

## 1. 전체 구성도

```
┌────────────────────────────┐
│        브라우저             │
│  http://localhost:5173     │
└─────────────┬──────────────┘
              │  (1) React 코드 / fetch('/api/...')
              ▼
┌────────────────────────────┐        (2) 정적 파일 + HMR
│  Vite 개발 서버 (:5173)     │◀───────────────────────────
│  · src/** 개발 서버 제공     │
│  · /api/** → 프록시          │
└─────────────┬──────────────┘
              │  (3) 프록시: /api/** → http://localhost:8080
              ▼
┌────────────────────────────┐
│  Spring Boot (:8080)       │
│  · HelloController         │  (4) JSON 응답
│  · ApiExceptionHandler     │
│  · CorsConfig              │
└────────────────────────────┘
```

- 개발 중에는 **Vite 프록시**를 사용하므로 브라우저 입장에서 `/api` 는 같은 오리진(5173)입니다 → **CORS 가 발생하지 않습니다.**
- 프론트엔드가 백엔드 주소를 직접 호출하는 상황(배포, 또는 `.env.local` 의 `VITE_API_BASE_URL` 지정)을 대비해 백엔드에 `CorsConfig` 를 준비해 두었습니다.

## 2. 왜 이렇게 구성했나

| 구성 요소                  | 이유                                                          |
| -------------------------- | ------------------------------------------------------------- |
| Vite `server.proxy`        | 개발 중 CORS·인증서 문제 없이 같은 오리진처럼 호출하기 위해   |
| `CorsConfig` (`/api/**`)   | 배포/직접 호출 시에도 프론트 오리진을 허용하기 위해           |
| `api/` 패키지 분리         | REST 엔드포인트(컨트롤러)를 한 곳에 모아 찾기 쉽게 하기 위해  |
| `api/dto` 에 `record` DTO  | 요청/응답 스키마를 명시적으로 표현하고 불변으로 유지하기 위해 |
| `ApiExceptionHandler`      | 검증 실패 응답 형식을 서버 전체에서 동일하게 유지하기 위해    |
| `config/` 패키지           | CORS, 보안 등 애플리케이션 설정을 컨트롤러와 분리하기 위해    |
| Maven Wrapper (`mvnw.cmd`) | 팀원/다른 PC 에서 Maven 설치 없이 동일 버전으로 빌드하기 위해 |

## 3. 요청 흐름 (시퀀스)

### 3.1 조회: `GET /api/hello`

```
[브라우저]                     [Vite :5173]                [Spring Boot :8080]
   │  App.jsx 마운트                │                              │
   │  fetchHello() 실행             │                              │
   │  GET /api/hello ──────────────▶│ ── 프록시(changeOrigin) ───▶ │
   │                                │                              │ HelloController.hello()
   │                                │                              │ → new HelloResponse(...)
   │                                │◀──── 200 {message, timestamp}│
   │◀── 응답 도착                    │                              │
   │  showHello(data) → setHello()  │                              │
   │  화면에 message/timestamp 표시  │                              │
```

프론트엔드 코드 경로: `App.jsx` → `src/api/greeting.js`(`fetchHello`) → `src/api/client.js`(`request`) → `fetch`

### 3.2 전송 + 검증 성공: `POST /api/hello/echo`

```
[브라우저] 폼 제출
  └─ handleSubmit(event) → sendEcho({name, message})
       └─ POST /api/hello/echo  body: {"name":"godsse","message":"  hello  "}
            └─ HelloController.echo(@Valid @RequestBody EchoRequest request)
                 ├─ Bean Validation 통과
                 ├─ message.trim() → "hello" (길이 5)
                 └─ 200 {"name":"godsse","message":"hello","length":5}
       └─ setEcho(...) → 결과 표 출력
```

### 3.3 전송 + 검증 실패(400)

```
POST /api/hello/echo  body: {"name":"", "message":"정상 메시지"}
  └─ @Valid 검증 실패 (name 이 @NotBlank 위반)
       └─ Spring 이 MethodArgumentNotValidException 발생
            └─ ApiExceptionHandler.handleValidation(...)
                 └─ 400
                    {
                      "message": "요청 값이 올바르지 않습니다.",
                      "errors": [ { "field": "name", "reason": "name 은 필수입니다." } ]
                    }
  └─ client.js 의 request() 가 response.ok == false → Error(message) throw
       └─ App.jsx 의 catch → setEchoError(...) → 화면에 오류 문구 표시
```

## 4. 백엔드 내부 레이어

```
HTTP 요청
  │
  ├─ DispatcherServlet (Spring MVC)
  │
  ├─ @RestController  ─ HelloController        : URL 매핑, 응답 반환
  │      └─ @Valid @RequestBody …  ─ dto/EchoRequest : 입력 스키마 + 검증 규칙
  │
  ├─ WebMvcConfigurer ─ CorsConfig             : /api/** 오리진/메서드/헤더 허용
  │
  ├─ @RestControllerAdvice ─ ApiExceptionHandler : 예외 → 일관된 JSON 응답
  │      └─ dto/ValidationErrorResponse, dto/FieldValidationError
  │
  └─ 설정 ─ application.properties             : 포트, CORS 오리진, actuator 노출 범위
```

지금은 컨트롤러가 데이터를 직접 만들어 반환하지만, 기능이 늘어나면 아래처럼 계층을 나누는 것을 권장합니다.

```
api(Controller) → service(비즈니스 로직) → repository(데이터 접근) → DB
```

## 5. 프론트엔드 렌더링/데이터 흐름

```
index.html (#root)
   └─ src/main.jsx  : createRoot(document.getElementById('root')).render(<StrictMode><App/></StrictMode>)
        └─ src/App.jsx
             ├─ useState : hello, helloError, loading / name, message, echo, echoError
             ├─ useEffect(최초 1회) : fetchHello().then(showHello).catch(showHelloError)
             ├─ handleReload()      : "다시 요청" 버튼
             └─ handleSubmit(event) : sendEcho({name, message}) → setEcho / setEchoError
```

- 데이터 로딩 함수는 `src/api/greeting.js` 에 모여 있고, 실제 HTTP 처리는 `src/api/client.js` 가 담당합니다.
- `StrictMode` 로 인해 개발 모드에서는 effect 가 두 번 실행될 수 있어, effect 안에서 `ignore` 플래그로 중복 반영을 막습니다.
- React Hooks 7 의 `set-state-in-effect` 규칙 때문에 effect 본문에서 곧바로 `setState` 를 호출하지 않고 `.then(callback)` 안에서 호출합니다. (자세한 내용은 [04-frontend.md](./04-frontend.md))

## 관련 문서

- 백엔드 상세 → [03-backend.md](./03-backend.md)
- 프론트엔드 상세 → [04-frontend.md](./04-frontend.md)
- API 명세 → [05-api.md](./05-api.md)
