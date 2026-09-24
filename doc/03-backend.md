# 03. 백엔드 (Spring Boot)

기준 경로: `backend/src/main/java/com/godsse/backend`

## 1. 패키지 구조

```
com.godsse.backend
├─ GodsseBackendApplication.java   # 진입점 (@SpringBootApplication)
├─ api/                            # REST 엔드포인트 계층 (Controller)
│  ├─ HelloController.java         # 샘플 API 3개 (연결 확인용)
│  ├─ ApiExceptionHandler.java     # @RestControllerAdvice (예외 → JSON)
│  └─ dto/                         # 요청/응답 스키마 (record)
│     ├─ HelloResponse.java
│     ├─ EchoRequest.java
│     ├─ EchoResponse.java
│     ├─ FieldValidationError.java
│     └─ ValidationErrorResponse.java
├─ service/                        # 비즈니스 로직 (기능 구현 단계에서 추가)
├─ domain/                         # 엔티티 = DB 테이블
│  ├─ BaseTimeEntity.java          # 생성/수정 시각 공통 상위 클래스
│  ├─ User.java                    # users
│  ├─ Follow.java                  # follows
│  ├─ Review.java                  # reviews (감상 메시지)
│  ├─ RuleSystem.java              # rule_systems
│  ├─ SessionPlan.java             # session_plans (달력)
│  └─ enums/
│     ├─ SpoilerMode.java          # NONE / BLUR / FOLD
│     ├─ Visibility.java           # PUBLIC / FOLLOWERS / PRIVATE
│     └─ SessionStatus.java        # PLANNED / DONE / CANCELED
├─ repository/                     # DB 접근 (Spring Data JPA 인터페이스)
│  ├─ UserRepository.java
│  ├─ FollowRepository.java
│  ├─ ReviewRepository.java
│  ├─ RuleSystemRepository.java
│  └─ SessionPlanRepository.java
└─ config/
   ├─ CorsConfig.java              # WebMvcConfigurer 로 /api/** CORS 설정
   ├─ WebConfig.java               # /uploads/** 를 업로드 폴더와 연결
   ├─ UploadConfig.java            # 업로드 폴더(Path) 빈 등록 + 폴더 생성
   └─ PasswordConfig.java          # BCrypt PasswordEncoder 빈 등록
```

- **계층 규칙**: 요청을 받는 `api` → 로직을 담는 `service` → DB 를 다루는 `repository` → 테이블과 대응하는 `domain`.
  기능이 커지면 컨트롤러에 로직을 넣지 않고 `service` 로 분리합니다.
- 테스트는 `backend/src/test/java/com/godsse/backend` 아래에 같은 패키지 규칙으로 둡니다.

## 2. 클래스별 역할

| 클래스 / 패키지 | 역할 |
| --- | --- |
| `GodsseBackendApplication` | `@SpringBootApplication` 진입점. `SpringApplication.run(...)` 실행 |
| `HelloController` | `/api/hello` 조회 2개 + `/api/hello/echo` 전송 1개 (연결 확인 샘플) |
| `ApiExceptionHandler` | `MethodArgumentNotValidException` → 400 + 표준 JSON. 기능 추가 시 401/404/409 매핑을 여기에 확장 |
| `CorsConfig` | `app.cors.allowed-origins` 값을 읽어 `/api/**` 에 CORS 허용 |
| `WebConfig` | `file:` 경로를 `/uploads/**` URL 로 제공(프로필 이미지) |
| `UploadConfig` | `app.upload.dir` 을 절대 경로 `Path` 빈으로 등록하고 폴더를 미리 만든다 |
| `PasswordConfig` | `BCryptPasswordEncoder` 빈 등록 (비밀번호 해시) |
| `domain/*` | 엔티티. 테이블 구조는 [07-domain-model.md](./07-domain-model.md) 참고 |
| `repository/*` | `JpaRepository` 를 상속한 인터페이스. 메서드 이름 규칙으로 쿼리 자동 생성 |

### 샘플 컨트롤러 동작 예 (HelloController)

```java
@RestController
@RequestMapping("/api/hello")
public class HelloController {

	@GetMapping
	public HelloResponse hello() {
		return new HelloResponse("godsse 백엔드에 연결되었습니다.", Instant.now());
	}

	@GetMapping("/{name}")
	public HelloResponse helloTo(@PathVariable String name) {
		return new HelloResponse(name + "님, 반갑습니다!", Instant.now());
	}

	@PostMapping("/echo")
	public EchoResponse echo(@Valid @RequestBody EchoRequest request) {
		String message = request.message().trim();
		return new EchoResponse(request.name(), message, message.length());
	}

}
```

> 코드 스타일: Spring 관례대로 **탭 인덴트**, 생성자 주입, DTO 는 `record`, 엔티티는 `@Entity` 클래스입니다. (Lombok 미사용)

## 3. 의존성 (`pom.xml`)

| 의존성 | 범위 | 용도 |
| --- | --- | --- |
| `spring-boot-starter-webmvc` | compile | REST 컨트롤러, JSON(Jackson) |
| `spring-boot-starter-validation` | compile | `@Valid`, `@NotBlank`, `@Size` |
| `spring-boot-starter-data-jpa` | compile | 엔티티 ↔ 테이블 매핑, 리포지토리 자동 구현 |
| `spring-security-crypto` | compile | **BCrypt 해시만** 사용 (전체 Security 필터 체인은 미도입) |
| `spring-boot-starter-actuator` | compile | `/actuator/health` 등 운영 엔드포인트 |
| `com.h2database:h2` | runtime | 개발용 파일 DB (별도 설치 불필요) |
| `spring-boot-h2console` | compile | H2 웹 콘솔(`/h2-console`). **Boot 4 부터는 별도 모듈** |

| `spring-boot-devtools` | runtime | 개발 중 자동 재시작(배포 시 제외) |
| `spring-boot-starter-webmvc-test` | test | MockMvc 등 웹 계층 테스트 |
| `spring-boot-starter-validation-test` | test | 검증 관련 테스트 지원 |
| `spring-boot-starter-data-jpa-test` | test | JPA 테스트 지원 |
| `spring-boot-starter-actuator-test` | test | Actuator 테스트 지원 |
| `spring-boot-maven-plugin` | build | `spring-boot:run`, 실행 가능 jar 패키징 |

- 부모: `org.springframework.boot:spring-boot-starter-parent:4.1.1`
- Java: `<java.version>17</java.version>`
- Spring Boot 4 의 테스트 스타터는 모듈별로 나뉘어 있습니다(`spring-boot-starter-<모듈>-test`).

## 4. 설정 (`application.properties`)

| 프로퍼티 | 값 | 설명 |
| --- | --- | --- |
| `spring.application.name` | `godsse-backend` | 애플리케이션 이름 |
| `server.port` | `8080` | HTTP 포트 |
| `app.cors.allowed-origins` | `http://localhost:5173` | CORS 허용 오리진(콤마로 여러 개) |
| `management.endpoints.web.exposure.include` | `health,info` | Actuator 노출 엔드포인트 제한 |
| `spring.datasource.url` | `jdbc:h2:file:./data/godsse` | 개발용 DB 파일 위치(`backend/data/`) |
| `spring.datasource.driver-class-name` | `org.h2.Driver` | H2 드라이버 |
| `spring.jpa.hibernate.ddl-auto` | `update` | 엔티티 변경을 테이블에 자동 반영(개발용) |
| `spring.jpa.show-sql` | `true` | 실행 SQL 콘솔 출력(학습용) |
| `spring.jpa.open-in-view` | `false` | 화면 렌더링까지 DB 커넥션을 붙잡지 않게 함 |
| `spring.h2.console.enabled` | `true` | `/h2-console` 웹 콘솔(개발용) |
| `app.upload.dir` | `./uploads` | 업로드 파일 저장 폴더 |
| `app.upload.max-bytes` | `2097152` | 업로드 1건 최대 크기(2MB) |
| `spring.servlet.multipart.max-file-size` | `2MB` | multipart 파일 크기 제한 |
| `spring.servlet.multipart.max-request-size` | `3MB` | multipart 요청 전체 제한 |

테스트 전용 설정은 `backend/src/test/resources/application.properties` 에서 **메모리 DB** 로 덮어씁니다.
(테스트가 개발용 파일 DB 를 건드리지 않도록)

## 5. 데이터베이스 (개발용 H2)

- 저장 위치: `backend/data/godsse.mv.db` (재시작해도 유지, `.gitignore` 로 제외)
- 웹 콘솔: <http://localhost:8080/h2-console>
  - JDBC URL: `jdbc:h2:file:./data/godsse`
  - 사용자: `sa`, 비밀번호: (빈 값)
- 테이블 구조: [07-domain-model.md](./07-domain-model.md)
- 운영 전환 시에는 `ddl-auto` 를 `validate` 로 바꾸고 PostgreSQL 등으로 옮기는 것을 검토합니다.

## 6. 테스트

| 파일 | 내용 |
| --- | --- |
| `GodsseBackendApplicationTests` | `@SpringBootTest` 컨텍스트가 정상 기동하는지 확인(`contextLoads`) |
| `api/HelloControllerTest` | `@SpringBootTest` + `@AutoConfigureMockMvc` 로 HTTP 계층 검증 4건 |
| `repository/UserRepositoryTest` | 엔티티 매핑/리포지토리 동작 검증 2건 (`@Transactional` 로 롤백) |

프로젝트 루트(`godsse`)에서 실행합니다.

```powershell
cd backend
.\mvnw.cmd test
```

> Spring Boot 4 에서는 웹 테스트 애노테이션 패키지가 변경되었습니다.
> `@WebMvcTest`, `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure`
> (`MockMvc` 자체는 Spring Framework 의 `org.springframework.test.web.servlet`)

## 7. 새 API 추가하는 방법

1. **엔티티 / 리포지토리** — 새 데이터라면 `domain` 과 `repository` 에 추가
2. **DTO 추가** — `api/dto` 에 `record` 로 요청/응답 정의 (검증은 `jakarta.validation.constraints.*`)
3. **서비스 추가** — `service` 에 로직 작성. 조회는 `@Transactional(readOnly = true)`, 변경은 `@Transactional`.
   **엔티티를 DTO 로 바꾸는 일은 이 안에서** 끝낸다(지연 로딩 문제 방지 — [study/01](./study/01-jpa-and-entity.md) 참고)
4. **컨트롤러 추가** — `api` 패키지에 `@RestController` + `@RequestMapping("/api/...")`
5. **테스트 추가** — `src/test/java/.../api` 에 `@SpringBootTest` + `@AutoConfigureMockMvc` 테스트
6. **문서 갱신** — [05-api.md](./05-api.md) 에 명세 추가

```java
// api/dto/GreetingRequest.java
public record GreetingRequest(@NotBlank(message = "name 은 필수입니다.") String name) {
}
```

```java
// api/GreetingController.java
@RestController
@RequestMapping("/api/greetings")
public class GreetingController {

	@GetMapping
	public List<String> list() {
		return List.of("hello", "hi");
	}

}
```

## 8. 실행/빌드 명령

| 목적 | 명령 |
| --- | --- |
| 개발 실행 | `.\mvnw.cmd spring-boot:run` |
| 테스트 | `.\mvnw.cmd test` |
| 빌드 | `.\mvnw.cmd clean package` → `target/godsse-backend-0.0.1-SNAPSHOT.jar` |
| jar 실행 | `java -jar target\godsse-backend-0.0.1-SNAPSHOT.jar` |

## 관련 문서

- 도메인/테이블 → [07-domain-model.md](./07-domain-model.md)
- 요청 흐름 → [02-architecture.md](./02-architecture.md)
- API 명세 → [05-api.md](./05-api.md)
- 실행 환경/트러블슈팅 → [06-development-guide.md](./06-development-guide.md)
- JPA 학습 노트 → [study/01-jpa-and-entity.md](./study/01-jpa-and-entity.md)
