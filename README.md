# godsse

React(Vite) 프론트엔드 + Java Spring Boot 백엔드로 구성된 풀스택 학습용 프로젝트입니다.

## 구조

```
godsse/
├─ frontend/   # React 19 + Vite (JavaScript)
├─ backend/    # Spring Boot 4.1.1 + Java 17 (Maven, Maven Wrapper 포함)
└─ doc/        # 프로젝트 문서 (구조 / 흐름 / API / 개발 가이드)
```

## 문서

프로젝트 구조와 동작 흐름은 [`doc/`](./doc/README.md) 폴더에 정리되어 있습니다.

| 문서                                                         | 내용                                             |
| ------------------------------------------------------------ | ------------------------------------------------ |
| [doc/README.md](./doc/README.md)                             | 문서 인덱스 + 30초 요약                          |
| [doc/01-overview.md](./doc/01-overview.md)                   | 프로젝트 개요, 기술 스택, 전체 디렉터리 구조     |
| [doc/02-architecture.md](./doc/02-architecture.md)           | 전체 구성도, 요청 흐름(시퀀스), 프록시/CORS 동작 |
| [doc/03-backend.md](./doc/03-backend.md)                     | 백엔드 패키지·클래스·의존성·설정·테스트          |
| [doc/04-frontend.md](./doc/04-frontend.md)                   | 컴포넌트 구조, 상태, API 계층, 린트/포맷 설정    |
| [doc/05-api.md](./doc/05-api.md)                             | API 명세(요청/응답/에러)와 호출 예시             |
| [doc/06-development-guide.md](./doc/06-development-guide.md) | 실행 순서, 검증, 트러블슈팅, 코드 규칙           |

## 사전 요구사항

| 도구    | 버전                      | 확인 명령       |
| ------- | ------------------------- | --------------- |
| Node.js | 20.19+ 또는 22.12+        | `node -v`       |
| JDK     | 17 (Spring Boot 4 기준)   | `java -version` |
| Maven   | 설치 불필요 (`mvnw` 사용) | -               |

JDK 가 없다면 아래 중 하나로 설치합니다.

```powershell
# winget 사용
winget install EclipseAdoptium.Temurin.17.JDK

# 또는 https://adoptium.net/temurin/releases/?version=17 에서 수동 설치
```

설치 후 새 터미널에서 `java -version` 이 17.x 로 표시되는지 확인하세요. (필요 시 `JAVA_HOME` 을 JDK 경로로 지정)

## 백엔드 실행 (http://localhost:8080)

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

- 최초 실행 시 Maven 과 의존성을 내려받기 때문에 시간이 조금 걸립니다.
- 테스트: `.\mvnw.cmd test`
- 빌드: `.\mvnw.cmd clean package` (결과물: `target/godsse-backend-0.0.1-SNAPSHOT.jar`)

## 프론트엔드 실행 (http://localhost:5173)

```powershell
cd frontend
npm install
npm run dev
```

- `npm run build` : 프로덕션 빌드 (`frontend/dist`)
- `npm run lint` : ESLint 검사 및 자동 수정
- `npm run format` : Prettier 포맷팅

개발 모드에서는 `frontend/vite.config.js` 의 프록시 설정으로 `/api` 요청이 `http://localhost:8080` 으로 전달되므로 CORS 없이 연동됩니다.

## 샘플 API

| 메서드 | 경로                | 설명                                  |
| ------ | ------------------- | ------------------------------------- |
| GET    | `/api/hello`        | 서버 인사 메시지와 timestamp 반환     |
| GET    | `/api/hello/{name}` | 이름이 포함된 인사 메시지 반환        |
| POST   | `/api/hello/echo`   | `name`, `message` 검증 후 메시지 반환 |
| GET    | `/actuator/health`  | 헬스체크                              |

`POST /api/hello/echo` 요청 예시:

```json
{ "name": "godsse", "message": "안녕하세요" }
```

검증 실패 시 400 과 함께 필드 오류 목록이 반환됩니다.

```json
{
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [{ "field": "name", "reason": "name 은 필수입니다." }]
}
```

## CORS

프론트엔드에서 백엔드를 직접 호출하는 경우를 대비해 `backend/src/main/resources/application.properties` 의
`app.cors.allowed-origins` 에 허용 오리진이 설정되어 있습니다. (기본값 `http://localhost:5173`)

## 참고

- Java/Spring 코드는 탭 인덴트, 프론트엔드 코드는 2칸 스페이스 + Prettier(semi 없음, single quote, 100자) 규칙을 사용합니다.
- VS Code 에서 Java 는 `Language Support for Java`(Red Hat), JS/React 는 ESLint + Prettier 확장 사용을 권장합니다.
