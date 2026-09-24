# 05. API 명세

> **현재 상태 (2026-09-24)**: 이 문서는 백엔드 연결 확인용 샘플 API 만 담고 있습니다.
> 로그인·프로필·팔로우·리뷰(감상)·피드 API 는 기능 구현 단계에서 이 문서에 추가합니다.
> 데이터 구조(테이블)는 [07-domain-model.md](./07-domain-model.md) 를 참고하세요.


## 1. 공통 사항

| 항목      | 내용                                                                |
| --------- | ------------------------------------------------------------------- |
| Base URL  | `http://localhost:8080` (개발 중 프론트엔드는 `/api` 상대경로 사용) |
| 요청 헤더 | `Content-Type: application/json`                                    |
| 응답 형식 | JSON (UTF-8)                                                        |
| 시간 형식 | ISO-8601 문자열, 예: `2026-09-23T12:34:56.789Z`                     |
| CORS 허용 | `http://localhost:5173` (`app.cors.allowed-origins`)                |

성공 응답은 각 엔드포인트의 DTO 그대로 내려가고(래퍼 없음), 실패 응답은 아래 형태로 통일됩니다.

```json
{
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [{ "field": "name", "reason": "name 은 필수입니다." }]
}
```

## 2. `GET /api/hello`

서버 연결 확인용 인사 메시지를 반환합니다.

- **응답 200**

```json
{
  "message": "godsse 백엔드에 연결되었습니다.",
  "timestamp": "2026-09-23T12:34:56.789Z"
}
```

| 필드        | 타입   | 설명                     |
| ----------- | ------ | ------------------------ |
| `message`   | string | 서버 고정 인사 메시지    |
| `timestamp` | string | 응답 생성 시각(ISO-8601) |

## 3. `GET /api/hello/{name}`

경로 변수로 받은 이름이 포함된 인사 메시지를 반환합니다.

- **요청 예**: `GET /api/hello/godsse`
- **응답 200**

```json
{
  "message": "godsse님, 반갑습니다!",
  "timestamp": "2026-09-23T12:34:56.789Z"
}
```

> 경로 변수는 URL 인코딩이 필요합니다. 프론트엔드의 `fetchHelloTo` 는 `encodeURIComponent` 를 사용합니다.

## 4. `POST /api/hello/echo`

전달받은 값을 검증한 뒤, 앞뒤 공백을 제거한 메시지와 길이를 돌려줍니다.

- **요청 본문**

| 필드      | 타입   | 필수 | 검증 규칙                                 |
| --------- | ------ | ---- | ----------------------------------------- |
| `name`    | string | Y    | `@NotBlank` — 빈 값/공백 불가             |
| `message` | string | Y    | `@NotBlank` + `@Size(min = 2, max = 100)` |

```json
{ "name": "godsse", "message": "  hello  " }
```

- **응답 200**

```json
{ "name": "godsse", "message": "hello", "length": 5 }
```

| 필드      | 타입   | 설명                      |
| --------- | ------ | ------------------------- |
| `name`    | string | 요청한 이름 그대로        |
| `message` | string | 앞뒤 공백을 제거한 메시지 |
| `length`  | number | 공백 제거 후 메시지 길이  |

- **응답 400 (검증 실패)**

```json
{
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [{ "field": "name", "reason": "name 은 필수입니다." }]
}
```

> `errors` 는 **위반한 제약 조건 단위**로 쌓입니다. 예를 들어 `message` 가 빈 문자열이면 `@NotBlank` 와 `@Size(min = 2)` 가 모두 위반되어 같은 필드가 2건 들어갑니다.

## 5. `GET /actuator/health`

```json
{ "status": "UP" }
```

- 노출 엔드포인트는 `application.properties` 의 `management.endpoints.web.exposure.include=health,info` 로 제한되어 있습니다.

## 6. 호출 예시

```powershell
# curl.exe 기준
curl.exe http://localhost:8080/api/hello
curl.exe "http://localhost:8080/api/hello/godsse"
curl.exe -X POST http://localhost:8080/api/hello/echo `
  -H "Content-Type: application/json" `
  -d "{\"name\":\"godsse\",\"message\":\"  hello  \"}"
curl.exe http://localhost:8080/actuator/health
```

```powershell
# PowerShell 기준
Invoke-RestMethod http://localhost:8080/api/hello
Invoke-RestMethod "http://localhost:8080/api/hello/godsse"
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/hello/echo `
  -ContentType 'application/json' -Body '{"name":"godsse","message":"  hello  "}'
```

> 한글 본문을 PowerShell 로 보낼 때는 인코딩 문제가 생길 수 있으므로 `curl.exe` 사용을 권장합니다.

## 7. 상태 코드 정리

| 상황                     | 상태 코드 | 응답                    |
| ------------------------ | --------- | ----------------------- |
| 정상 조회/처리           | 200       | 각 DTO JSON             |
| 요청 검증 실패(`@Valid`) | 400       | `{ message, errors[] }` |
| 존재하지 않는 경로       | 404       | Spring 기본 오류 응답   |
| 서버 내부 오류           | 500       | Spring 기본 오류 응답   |

## 관련 문서

- 요청 흐름 → [02-architecture.md](./02-architecture.md)
- 백엔드 구현 상세 → [03-backend.md](./03-backend.md)
- 실행 가이드 → [06-development-guide.md](./06-development-guide.md)
