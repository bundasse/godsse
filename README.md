# godsse

React(Vite) 프론트엔드 + Java Spring Boot 백엔드로 구성된 풀스택 학습용 프로젝트입니다.

## 프로젝트 작업 대원칙 (최우선 · 필독)

> 상세: [doc/00-principles.md](./doc/00-principles.md)
>
> 1. **한국어 우선** — 보고·질문·설명은 한국어로. 한국어로 적확한 설명이 불가능할 때만 영어로.
> 2. **계획 → 승인 → 실행** — 지시받으면 먼저 계획(변경 파일/내용, 검증 방법, 리스크)을 보고하고,
>    **승인 후에만** 파일을 수정한다. (읽기·검색은 예외)
> 3. **사용자 배경 고려** — 사용자는 Vue 프론트엔드 개발자이며 백엔드·Java·Spring·CS 는 초급.
>    프론트엔드 개념에 비유해 설명하고, 용어는 `한국어(영어 원어)` + 한 줄 정의로 소개.
> 4. **학습 자료화** — 백엔드·Java·Spring·CS 내용은 `doc/study/` 에 노트로 정리한 뒤 읽어보라고 요청.

AI 도구 자동 로드 파일: [`.clinerules/00-principles.md`](./.clinerules/00-principles.md) · [`AGENTS.md`](./AGENTS.md)

## 구조

```
godsse/
├─ frontend/      # React 19 + Vite (JavaScript)
├─ backend/       # Spring Boot 4.1.1 + Java 17 (Maven, Maven Wrapper 포함)
├─ doc/           # 프로젝트 문서 (대원칙 / 구조 / 흐름 / API / 개발 가이드)
│  └─ study/      # 학습 노트 (백엔드 · Java · Spring · 컴퓨터공학)
├─ .clinerules/   # AI 도구용 작업 규칙 (Cline 자동 로드)
└─ AGENTS.md      # AI 도구 공통 작업 지침
```

## 문서

프로젝트 구조와 동작 흐름은 [`doc/`](./doc/README.md) 폴더에 정리되어 있습니다.

| 문서                                                         | 내용                                             |
| ------------------------------------------------------------ | ------------------------------------------------ |
| [doc/00-principles.md](./doc/00-principles.md)               | **작업 대원칙(최우선)** — 필독                   |
| [doc/README.md](./doc/README.md)                             | 문서 인덱스 + 30초 요약                          |
| [doc/01-overview.md](./doc/01-overview.md)                   | 프로젝트 개요, 기술 스택, 전체 디렉터리 구조     |
| [doc/02-architecture.md](./doc/02-architecture.md)           | 전체 구성도, 요청 흐름(시퀀스), 프록시/CORS 동작 |
| [doc/03-backend.md](./doc/03-backend.md)                     | 백엔드 패키지·클래스·의존성·설정·테스트          |
| [doc/04-frontend.md](./doc/04-frontend.md)                   | 컴포넌트 구조, 상태, API 계층, 린트/포맷 설정    |
| [doc/05-api.md](./doc/05-api.md)                             | API 명세(요청/응답/에러)와 호출 예시             |
| [doc/06-development-guide.md](./doc/06-development-guide.md) | 실행 순서, 검증, 트러블슈팅, 코드 규칙           |
| [doc/07-domain-model.md](./doc/07-domain-model.md)           | 도메인 모델(ERD·테이블·스포일러 규칙)            |

| [doc/study/README.md](./doc/study/README.md)                 | 학습 노트 인덱스 (백엔드·Java·Spring·CS)         |

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

### VS Code 로 실행 (권장)

`.vscode/launch.json` 과 `.vscode/tasks.json` 이 준비되어 있습니다.

| 하고 싶은 일 | 방법 |
| --- | --- |
| 백엔드 · 프론트엔드 각각 실행 | 실행 및 디버그(F5) → `백엔드: 실행 (Spring Boot)` / `프론트엔드: 실행 (Vite dev)` |
| 둘 다 한 번에 | 실행 및 디버그 → `풀스택: 백엔드 + 프론트엔드` |
| 빌드 · 테스트 | 터미널 → 태스크 실행 → `backend: 빌드 (jar)` / `backend: 테스트` / `frontend: 빌드` / `검증: 프론트엔드 (포맷 → 린트 → 빌드)` |

## 샘플 API

| 메서드 | 경로                | 설명                                  |
| ------ | ------------------- | ------------------------------------- |
| GET    | `/api/hello`        | 서버 인사 메시지와 timestamp 반환     |
| GET    | `/api/hello/{name}` | 이름이 포함된 인사 메시지 반환        |
| POST   | `/api/hello/echo`   | `name`, `message` 검증 후 메시지 반환 |
| GET    | `/actuator/health`  | 헬스체크                              |
| POST   | `/api/auth/signup`  | 회원가입(핸들·이메일·비밀번호·닉네임) |
| POST   | `/api/auth/login`   | 로그인(핸들·비밀번호)                 |
| POST   | `/api/auth/logout`  | 로그아웃 (204, 본문 없음)             |
| GET    | `/api/auth/me`      | 로그인한 사용자 정보 (미로그인 시 401) |

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
