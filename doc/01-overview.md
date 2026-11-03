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
├─ README.md, AGENTS.md, .editorconfig, .gitattributes, .gitignore
├─ .clinerules/                   # AI 작업 규칙(대원칙 4가지)
├─ doc/                           # ★ 설계·API·학습 문서 (00~07, study/)
├─ backend/                       # ── Spring Boot (Java 17, Boot 4.1.1) ──
│  ├─ pom.xml, mvnw, mvnw.cmd
│  └─ src/main/java/com/godsse/backend/
│     ├─ api/                     # REST 컨트롤러 9개 + dto/ (record) + ApiExceptionHandler
│     ├─ service/                 # 비즈니스 로직·트랜잭션 경계 9개 (+ SSE 스트림/리스너)
│     ├─ repository/              # Spring Data JPA 10개
│     ├─ domain/                  # 엔티티 10개 + enums/
│     ├─ common/                  # @LoginUser, 예외 5종, 배지·스포일러·핸들·룰 색 규칙
│     └─ config/                  # CORS · 비밀번호 · 업로드 · 룰 시더
└─ frontend/                      # ── React 19 + Vite 8 ──
   └─ src/
      ├─ api/                     # client.js(공통 fetch 래퍼) + 도메인별 8개
      ├─ pages/                   # 화면 15개 (홈·검색·감상·마이·알림 …)
      ├─ components/              # 재사용 UI 21개
      ├─ layouts/, context/, constants/
      └─ App.jsx, main.jsx
```

> 파일 하나하나의 역할은 [03-backend.md](./03-backend.md) · [04-frontend.md](./04-frontend.md) 에 정리되어 있습니다.


## 4. 포트와 주소

| 대상                   | 주소                                  | 설명                                      |
| ---------------------- | ------------------------------------- | ----------------------------------------- |
| 프론트엔드 개발 서버   | http://localhost:5173                 | Vite (`vite.config.js` 의 `server.port`)  |
| 백엔드 API             | http://localhost:8080                 | `application.properties` 의 `server.port` |
| 백엔드 공개 API 확인    | http://localhost:8080/api/rules       | 룰 목록(로그인 불필요)                    |
| 헬스체크               | http://localhost:8080/actuator/health | Actuator                                  |
| 프론트 → 백엔드 프록시 | `/api/**` → `http://localhost:8080`   | Vite dev server proxy                     |

## 5. 현재 검증 상태

| 항목                       | 상태                                                    |
| -------------------------- | ------------------------------------------------------- |
| 프론트엔드 `npm install`   | ✅ 완료 (취약점 0)                                      |
| 프론트엔드 `npm run lint`  | ✅ 통과 (오류 0)                                        |
| 프론트엔드 `npm run build` | ✅ 성공 (75 modules)                                    |
| 프론트엔드 개발 서버       | ✅ `http://localhost:5173` HTTP 200 응답 확인           |
| 백엔드 컴파일/테스트       | ✅ `.\mvnw.cmd test` — **125건 통과** (2026-11-03)      |
| 실서버 API E2E             | ✅ 인증·감상·알림(SSE)·세션(참가자·통계·히트맵·룰 색)·룰 (2026-11-03) |
| 프론트엔드 테스트          | ⚠️ **없음** — 테스트 스크립트 미도입                    |

> 명령과 절차는 [06-development-guide.md](./06-development-guide.md) 참고.

## 관련 문서

- 요청이 흘러가는 과정 → [02-architecture.md](./02-architecture.md)
- API 상세 → [05-api.md](./05-api.md)
