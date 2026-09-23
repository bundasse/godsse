# 06. 개발 가이드 (실행·검증·규칙)

## 1. 사전 요구사항

| 도구    | 버전                         | 확인 명령       | 비고                         |
| ------- | ---------------------------- | --------------- | ---------------------------- |
| Node.js | 20.19 이상 또는 22.12 이상   | `node -v`       | Vite 8 요구사항              |
| npm     | Node 설치 시 함께 설치       | `npm -v`        |                              |
| JDK     | **17**                       | `java -version` | Spring Boot 4.1.1 기준       |
| Maven   | 설치 불필요(`mvnw.cmd` 사용) | -               | Wrapper 3.3.4 / Maven 3.9.16 |

JDK 가 없다면:

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
# 설치 후 새 터미널에서 확인
java -version
```

- `java` 명령이 인식되지 않으면 새 터미널을 열거나 `JAVA_HOME` 을 JDK 설치 경로로 지정하세요.
- 다른 배포판도 가능합니다(Zulu 17, Amazon Corretto 17 등). **버전 17** 만 맞추면 됩니다.

## 2. 실행 순서

### 2.1 백엔드 먼저 실행

아래 명령은 프로젝트 루트(`godsse`)에서 실행합니다.

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

- 최초 실행 시 Maven 과 의존성을 내려받으므로 수 분이 걸릴 수 있습니다.
- `Started GodsseBackendApplication ...` 로그가 보이면 준비 완료입니다.
- 확인: <http://localhost:8080/api/hello> , <http://localhost:8080/actuator/health>

### 2.2 프론트엔드 실행

```powershell
cd frontend
npm install      # 최초 1회
npm run dev
```

- 브라우저에서 <http://localhost:5173> 접속 → 서버 메시지와 폼이 보이면 연동 성공입니다.
- 백엔드를 끄면 화면에 "백엔드에 연결하지 못했습니다" 오류가 표시됩니다(정상 동작).

## 3. 검증 체크리스트

아래 명령은 프로젝트 루트(`godsse`)에서 실행합니다.

### 프론트엔드

```powershell
cd frontend
npm install                 # 의존성 설치
npm run format              # Prettier 적용
npx prettier --check src/   # 포맷 확인 ("All matched files use Prettier code style!")
npm run lint                # ESLint (오류 없이 종료)
npm run build               # dist/ 생성
npm run dev                 # http://localhost:5173 접속 확인
```

### 백엔드 (JDK 17 필요)

```powershell
cd backend
.\mvnw.cmd test             # 컨텍스트 로드 + HelloControllerTest 4건
.\mvnw.cmd spring-boot:run  # 8080 기동 후 curl 로 확인
```

| 체크 항목                   | 기대 결과                        |
| --------------------------- | -------------------------------- |
| `GET /api/hello`            | 200 + `message`, `timestamp`     |
| `GET /api/hello/godsse`     | 200 + `"godsse님, 반갑습니다!"`  |
| `POST /api/hello/echo` 정상 | 200 + `length` 동일 여부         |
| `POST /api/hello/echo` 오류 | 400 + `errors[0].field = "name"` |
| 프론트 화면                 | 메시지 표시, 폼 전송 결과 표시   |

## 4. 트러블슈팅

| 증상                                                | 원인                                        | 해결                                                                       |
| --------------------------------------------------- | ------------------------------------------- | -------------------------------------------------------------------------- |
| `java` 명령을 찾을 수 없음                          | JDK 미설치 / PATH 미반영                    | `winget install EclipseAdoptium.Temurin.17.JDK` 후 새 터미널               |
| `mvnw.cmd` 실행 시 JDK 관련 오류                    | `JAVA_HOME` 미설정                          | `JAVA_HOME` 을 JDK 17 경로로 지정                                          |
| 화면에 "백엔드에 연결하지 못했습니다"               | 백엔드 미실행                               | 백엔드를 먼저 실행(8080)                                                   |
| `Port 8080 was already in use`                      | 다른 앱이 8080 점유                         | `netstat -ano \| findstr :8080` 으로 확인 후 종료하거나 `server.port` 변경 |
| 5173 포트 충돌                                      | 다른 Vite 서버 실행 중                      | Vite 가 자동으로 다음 포트로 뜸(프록시 동작은 동일)                        |
| 브라우저 콘솔 CORS 오류                             | 프론트가 백엔드를 직접 호출                 | `app.cors.allowed-origins` 에 해당 오리진 추가                             |
| ESLint `react-hooks/set-state-in-effect`            | effect 본문에서 동기 `setState` 호출        | `.then(callback)` 또는 이벤트 핸들러로 이동                                |
| ESLint `react-refresh/only-export-components`       | 컴포넌트 파일에서 컴포넌트 외 값 export     | 상수/유틸은 별도 파일로 분리                                               |
| Maven 빌드 시 parent 를 찾지 못함(404)              | `4.1.1.RELEASE` 는 Maven Central 에 없음    | pom 의 부모 버전을 `4.1.1` 로 유지                                         |
| `npm create vite` 가 "Ok to proceed? (y)" 에서 멈춤 | 비대화형 환경에서 패키지 설치 확인 프롬프트 | `--yes` 옵션을 주거나 파일을 직접 작성                                     |

## 5. 코드 규칙

| 항목        | 규칙                                                                     |
| ----------- | ------------------------------------------------------------------------ |
| 인덴트      | JS/JSX/CSS 2칸 스페이스, Java/XML 탭 (`.editorconfig`)                   |
| 포맷        | Prettier — 세미콜론 없음, 홑따옴표, 100자 (`frontend/.prettierrc.json`)  |
| Java 스타일 | 생성자 주입, `record` DTO, 필드 주입 금지, 패키지 `com.godsse.backend.*` |
| 패키지 분리 | 컨트롤러 `api`, 설정 `config`, 요청/응답 `api/dto`                       |
| 프론트 분리 | HTTP 공통 처리 `src/api/client.js`, 도메인별 API `src/api/*.js`          |
| 문서        | API 변경 시 `doc/05-api.md`, 구조 변경 시 `doc/01·02` 함께 갱신          |

커밋 전 체크리스트:

1. `npm run format` → `npm run lint` → `npm run build` (프론트)
2. `.\mvnw.cmd test` (백엔드, JDK 설치 후)
3. 새 API/화면이면 `doc/` 문서 갱신

## 6. 변경 이력

| 날짜       | 내용                                                                                                             |
| ---------- | ---------------------------------------------------------------------------------------------------------------- |
| 2026-09-23 | 최초 생성 — `godsse` 루트, `frontend`(React 19 + Vite 8), `backend`(Spring Boot 4.1.1, Java 17), `doc` 폴더 추가 |
| 2026-09-23 | Java 기준을 **17** 로 설정 (`pom.xml` → `<java.version>17</java.version>`)                                       |
| 2026-09-23 | pom 부모 버전을 `4.1.1.RELEASE` → `4.1.1` 로 수정(Maven Central 배포본에 맞춤)                                   |
| 2026-09-23 | 린트 구성은 create-vite 최신 템플릿(oxlint) 대신 기존 프로젝트와 동일한 **ESLint + Prettier** 로 통일            |

## 7. 남은 작업 (TODO)

- [ ] JDK 17 설치 후 `.\mvnw.cmd test` 로 백엔드 컴파일/테스트 검증 (현재 미검증)
- [ ] 필요 시 `git init` 후 첫 커밋 (`.gitignore`/`.gitattributes` 는 준비됨)
- [ ] 기능 확장 시 `service` 레이어, 라우팅(`react-router`), DB(JPA) 도입 검토

## 관련 문서

- 전체 구조 → [01-overview.md](./01-overview.md)
- 아키텍처/흐름 → [02-architecture.md](./02-architecture.md)
- API 명세 → [05-api.md](./05-api.md)
