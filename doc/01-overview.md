# 01. 프로젝트 개요와 구조

## 1. 목적

- React(프론트엔드) + Spring Boot(백엔드) 조합의 **풀스택 기본 골격**을 익히기 위한 학습용 프로젝트입니다.
- 두 애플리케이션이 실제로 통신하는 최소한의 예제(조회/검증/에러 처리)를 포함합니다.
- 이후 기능을 붙일 때 그대로 확장할 수 있도록 레이어와 규칙을 정리해 두었습니다.

## 2. 기술 스택

### 프론트엔드

| 항목                        | 버전                                    | 비고                              |
| --------------------------- | --------------------------------------- | --------------------------------- |
| React                       | 19.3.0                                  | `react`, `react-dom`              |
| Vite                        | 8.3.0                                   | 개발 서버 + 번들러                |
| @vitejs/plugin-react        | 6.1.1                                   | React Fast Refresh                |
| Node.js                     | 20.19 이상 또는 22.12 이상              | `package.json` 의 `engines` 참고  |
| ESLint                      | 10.11.0                                 | flat config (`eslint.config.js`)  |
| eslint-plugin-react-hooks   | 7.1.1                                   | React Hooks / React Compiler 규칙 |
| eslint-plugin-react-refresh | 0.5.7                                   | 컴포넌트 export 규칙              |
| Prettier                    | 3.9.9 (+ eslint-config-prettier 10.1.8) | 포맷터와 린트 충돌 방지           |
| react-router                 | 7.18.4                                  | 화면 라우팅 (`/u/:handle`, `/reviews/:id`) |


### 백엔드

| 항목            | 버전                                 | 비고                                          |
| --------------- | ------------------------------------ | --------------------------------------------- |
| Java            | **17**                               | `pom.xml` → `<java.version>17</java.version>` |
| Spring Boot     | **4.1.1**                            | `spring-boot-starter-parent`                  |
| Spring MVC      | starter-webmvc                       | REST 컨트롤러 (`@RestController`)             |
| Bean Validation | starter-validation                   | `@Valid`, `@NotBlank`, `@Size`                |
| Actuator        | starter-actuator                     | `/actuator/health`                            |
| DevTools        | spring-boot-devtools                 | 코드 변경 시 자동 재시작 (개발용)             |
| Maven           | 3.9.16 (Wrapper 3.3.4 자동 다운로드) | Maven 설치 불필요                             |
| Spring Data JPA | Boot 4.1.1 관리                        | `spring-boot-starter-data-jpa` (엔티티 ↔ 테이블) |
| H2              | Boot 4.1.1 관리                        | 개발용 파일 DB(설치 불필요), 테스트는 메모리 DB |
| spring-security-crypto | 7.x                            | BCrypt 비밀번호 해시만 사용(필터 체인 미도입)   |


## 3. 전체 디렉터리 구조

```
godsse/
├─ README.md                     # 프로젝트 소개/실행 방법
├─ .editorconfig                 # 인덴트 규칙 (js 2칸, java/xml 탭)
├─ .gitattributes                # 줄바꿈 규칙 (* text=auto eol=lf)
├─ .gitignore                    # node_modules, target, dist, .env 등 제외
├─ .vscode/
│  ├─ settings.json              # Prettier 기본 포매터, java/xml 탭 설정
│  └─ extensions.json            # 추천 확장(ESLint, Prettier, Java, Boot)
├─ doc/                          # ★ 이 문서 폴더
└─ backend/                      # ── Spring Boot ──
│  ├─ pom.xml                    # 의존성/버전 (Java 17, Boot 4.1.1)
│  ├─ mvnw, mvnw.cmd             # Maven Wrapper (Maven 설치 불필요)
│  ├─ .mvn/wrapper/maven-wrapper.properties
│  ├─ .gitignore, .gitattributes, HELP.md
│  └─ src/
│     ├─ main/java/com/godsse/backend/
│     │  ├─ GodsseBackendApplication.java      # 진입점(@SpringBootApplication)
│     │  ├─ api/HelloController.java           # 샘플 REST API
│     │  ├─ api/ApiExceptionHandler.java       # 검증 실패 → 400 JSON 변환
│     │  ├─ api/dto/*.java                     # 요청/응답 record DTO 5종
│     │  └─ config/CorsConfig.java             # /api/** CORS 설정
│     ├─ main/resources/application.properties # 포트, CORS, actuator 설정
│     └─ test/java/com/godsse/backend/
│        ├─ GodsseBackendApplicationTests.java # 컨텍스트 로드 테스트
│        └─ api/HelloControllerTest.java       # MockMvc API 테스트 4개
└─ frontend/                     # ── React (Vite) ──
   ├─ package.json               # 스크립트 + 의존성
   ├─ vite.config.js             # 포트/프록시/별칭 설정
   ├─ eslint.config.js           # ESLint flat config
   ├─ .prettierrc.json           # semi 없음, single quote, 100자
   ├─ .editorconfig, .env.example, .gitignore, jsconfig.json
   ├─ index.html                 # #root + /src/main.jsx 로드
   ├─ public/favicon.svg
   └─ src/
      ├─ main.jsx                # createRoot 로 App 마운트
      ├─ App.jsx                 # 예제 화면(조회 + 전송 폼)
      ├─ App.css, index.css      # 스타일
      └─ api/
         ├─ client.js            # 공통 fetch 래퍼 + 에러 처리
         └─ greeting.js          # /api/hello 관련 API 함수
```

> **2026-09-24 추가**: 프론트엔드는 `src/layouts/`(공통 레이아웃)·`src/pages/`(화면)·`src/components/`·
> `src/constants/` 로 확장되었고, 백엔드는 `domain/`(엔티티)·`repository/`(DB 접근) 가 추가되었습니다.
> 최신 구조는 [04-frontend.md](./04-frontend.md) · [03-backend.md](./03-backend.md) 를 참고하세요.


## 4. 포트와 주소

| 대상                   | 주소                                  | 설명                                      |
| ---------------------- | ------------------------------------- | ----------------------------------------- |
| 프론트엔드 개발 서버   | http://localhost:5173                 | Vite (`vite.config.js` 의 `server.port`)  |
| 백엔드 API             | http://localhost:8080                 | `application.properties` 의 `server.port` |
| 백엔드 API 문서(샘플)  | http://localhost:8080/api/hello       | `HelloController`                         |
| 헬스체크               | http://localhost:8080/actuator/health | Actuator                                  |
| 프론트 → 백엔드 프록시 | `/api/**` → `http://localhost:8080`   | Vite dev server proxy                     |

## 5. 현재 검증 상태

| 항목                       | 상태                                                    |
| -------------------------- | ------------------------------------------------------- |
| 프론트엔드 `npm install`   | ✅ 완료 (142 packages, 취약점 0)                        |
| 프론트엔드 `npm run lint`  | ✅ 통과 (오류 0)                                        |
| 프론트엔드 `npm run build` | ✅ 성공 (`19 modules transformed`)                      |
| 프론트엔드 개발 서버       | ✅ `http://localhost:5173` HTTP 200 응답 확인           |
| 백엔드 컴파일/테스트       | ⚠️ **미검증** — 개발 PC 에 JDK 가 없어 `mvnw` 실행 불가 |
| 프론트 `npm run build`(2026-09-24 재확인) | ✅ 성공 (40 modules, 라우팅 추가 후)                  |
| 백엔드 도메인/리포지토리 골격             | ⚠️ **미검증** — 개발 PC 에 JDK 가 없어 컴파일 불가    |


> 백엔드는 JDK 17 설치 후 `.\mvnw.cmd test` 로 검증하세요. 절차는 [06-development-guide.md](./06-development-guide.md) 참고.

## 관련 문서

- 요청이 흘러가는 과정 → [02-architecture.md](./02-architecture.md)
- API 상세 → [05-api.md](./05-api.md)
