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

### 2.3 VS Code 에서 실행 (권장)

`.vscode/launch.json`(실행·디버그)과 `.vscode/tasks.json`(반복 작업)이 준비되어 있습니다.

| 하고 싶은 일 | 방법 |
| --- | --- |
| 백엔드 실행(디버그) | 실행 및 디버그(F5) → **백엔드: 실행 (Spring Boot)** |
| 프론트엔드 실행 | 실행 및 디버그 → **프론트엔드: 실행 (Vite dev)** |
| 둘 다 한 번에 | 실행 및 디버그 → **풀스택: 백엔드 + 프론트엔드** |
| 빌드 결과 미리보기 | 실행 및 디버그 → **프론트엔드: 미리보기 (빌드 결과)** |
| 백엔드 빌드(jar) | 터미널 → 태스크 실행 → **backend: 빌드 (jar)** (Ctrl+Shift+B 기본 빌드) |
| 백엔드 테스트 | 태스크 실행 → **backend: 테스트** |
| 프론트 검증 묶음 | 태스크 실행 → **검증: 프론트엔드 (포맷 → 린트 → 빌드)** |

- 백엔드 실행 구성은 작업 폴더를 `backend` 로 잡아 둡니다(DB 파일이 `backend/data` 에 생기도록).
- 프론트엔드 실행 구성은 Windows 기준 `npm.cmd` 를 씁니다. (macOS/Linux 에서는 `npm`)
- 기동이 끝나면 브라우저가 자동으로 열립니다(`serverReadyAction`).


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
| `UserRepositoryTest`        | 저장·조회 및 생성/수정 시각 자동 기록 통과 |
| H2 콘솔                     | `/h2-console` 접속 후 `USERS`·`REVIEWS` 등 테이블 생성 확인 |
| 프론트 라우팅               | 360px / 768px / 1280px 에서 헤더 메뉴 ↔ 하단 탭 전환이 정상 |


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
| `Table "USERS" not found`                              | 엔티티 스캔 실패 또는 DDL 미실행                          | `@Entity` 가 `com.godsse.backend.domain` 아래에 있는지, `spring.jpa.hibernate.ddl-auto=update` 인지 확인 |
| H2 콘솔 접속 실패                                       | JDBC URL 불일치                                          | `/h2-console` 에서 JDBC URL 을 `application.properties` 의 `spring.datasource.url` 과 동일하게 입력 |
| `LazyInitializationException`                           | 트랜잭션 밖에서 지연 로딩 객체를 읽음                     | 조회를 `@Transactional` 서비스 안에서 DTO 로 변환 ([study/01](./study/01-jpa-and-entity.md) 참고) |
| `data/`·`uploads/` 가 git 에 올라감                     | 무시 목록 확인 부족                                      | `backend/.gitignore` 에 포함됨. 이미 추적 중이면 `git rm -r --cached data uploads` |
| 컴파일 오류 `illegal unicode escape`                    | Java 주석/문자열의 `백슬래시 + u` 조합을 javac 이 유니코드 이스케이프로 해석함 | 경로 예시는 슬래시(`/`)로 적는다. 꼭 필요하면 `\\u` 처럼 백슬래시를 두 번 쓴다 |
| H2 콘솔이 404 (`/h2-console` 접속 불가)                  | Spring Boot 4 부터 H2 콘솔이 **별도 모듈**로 분리됨        | `spring-boot-h2console` 의존성을 추가한다. 기동 로그에 `H2 console available at ...` 이 보이면 정상 |
| 테스트에서 `Could not resolve placeholder 'app.cors.allowed-origins'` | `src/test/resources/application.properties` 가 같은 이름이라 기본 설정을 **통째로 대체**함 | 테스트 덮어쓰기는 `application-test.properties` + `@ActiveProfiles("test")` 로 한다 |
| 설정을 고쳐도 이전 설정이 계속 적용됨                    | `target/test-classes` 에 이전 리소스가 남아 있음          | `.\mvnw.cmd clean test` 로 정리 후 실행                                    |
| PowerShell 에서 `curl.exe -d '{...}'` 본문이 깨져 400 (JSON parse error) | PowerShell 이 따옴표를 벗겨 전달함 | 본문을 UTF-8 파일로 저장하고 `--data-binary "@파일"` 로 보낸다 ([05-api.md](./05-api.md) 2.5 참고) |





## 5. 코드 규칙

| 항목        | 규칙                                                                     |
| ----------- | ------------------------------------------------------------------------ |
| 인덴트      | JS/JSX/CSS 2칸 스페이스, Java/XML 탭 (`.editorconfig`)                   |
| 포맷        | Prettier — 세미콜론 없음, 홑따옴표, 100자 (`frontend/.prettierrc.json`)  |
| Java 스타일 | 생성자 주입, `record` DTO, 필드 주입 금지, 패키지 `com.godsse.backend.*` |
| 패키지 분리 | 컨트롤러 `api`, 설정 `config`, 요청/응답 `api/dto`                       |
| 백엔드 계층 | 컨트롤러 `api`, 로직 `service`, DB 접근 `repository`, 엔티티 `domain`, 공통·예외 `common` |

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
| 2026-09-24 | 도메인·리포지토리 계층 추가 — `domain`(User/Follow/Review/RuleSystem/SessionPlan + enum 3종), `repository` 5종 |
| 2026-09-24 | 개발용 DB 도입 — Spring Data JPA + H2 파일 DB(`backend/data/godsse.mv.db`), 테스트는 메모리 DB로 분리 |
| 2026-09-24 | 비밀번호 암호화용 `spring-security-crypto`(BCrypt) 추가 (Spring Security 필터 체인은 미도입) |
| 2026-09-24 | 프로필 이미지 업로드 기반 — `app.upload.dir`, `/uploads/**` 정적 제공(`WebConfig`, `UploadConfig`) |
| 2026-09-24 | 프론트 라우팅 도입 — `react-router@7.18.4`, `layouts/`·`pages/`·`components/`·`constants/` 구조, 반응형 레이아웃 |
| 2026-09-24 | 문서 추가 — [07-domain-model.md](./07-domain-model.md), [study/01-jpa-and-entity.md](./study/01-jpa-and-entity.md) |
| 2026-09-25 | 회원가입·로그인·로그아웃·내 정보 API 구현 — `AuthService`, `AuthController`, 세션(HttpSession) + BCrypt, `@LoginUser` ArgumentResolver |
| 2026-09-25 | 오류 처리 확장 — `BusinessException`(404/409/401) 표준 응답, 잘못된 JSON 400 처리, 스택 트레이스 노출 차단(`server.error.*`) |
| 2026-09-25 | 프론트 인증 — `context/AuthProvider` + `useAuth`, `ProtectedRoute`, 로그인·회원가입 폼, `ApiError`(status/errors) |
| 2026-09-25 | VS Code 실행 구성 — `.vscode/launch.json`(백엔드/프론트엔드/풀스택), `.vscode/tasks.json`(실행·테스트·빌드·검증) |
| 2026-09-25 | 검증: 백엔드 테스트 **15건 통과**, 프론트 빌드 성공, 실서버 E2E(가입 201 → me 200 → 로그아웃 204 → me 401 → 로그인 200 → 중복 409) 확인 |



## 7. 남은 작업 (TODO)

- [ ] JDK 17 설치 후 `.\mvnw.cmd test` 로 백엔드 컴파일/테스트 검증 (현재 미검증)
- [ ] 필요 시 `git init` 후 첫 커밋 (`.gitignore`/`.gitattributes` 는 준비됨)
- [x] DB(JPA)·라우팅(react-router) 도입 — 2026-09-24 완료
- [ ] `service` 계층 정리 — 다음 단계(1-1 인증)에서 함께 도입
- [ ] 인증 인터셉터 + 로그인 사용자 주입(`@LoginUser`) 추가
- [ ] 예외 응답 확장(401/403/404/409) + 공통 예외 클래스
- [ ] 세션 로그인 보안 강화 — HttpOnly·SameSite 쿠키, 로그인 시 세션 ID 재발급
- [ ] 배포 준비 — DB 이전(PostgreSQL 등), `ddl-auto=validate`, 백업, HTTPS


## 관련 문서

- 전체 구조 → [01-overview.md](./01-overview.md)
- 아키텍처/흐름 → [02-architecture.md](./02-architecture.md)
- API 명세 → [05-api.md](./05-api.md)
