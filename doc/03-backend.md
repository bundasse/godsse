# 03. 백엔드 (Spring Boot)

## 1. 패키지 구조

기준 경로: `backend/src/main/java/com/godsse/backend`

```
com.godsse.backend
├─ GodsseBackendApplication.java   # 진입점 (@SpringBootApplication)
├─ api/                            # REST 엔드포인트 계층
│  ├─ HelloController.java         # 샘플 API 3개
│  ├─ ApiExceptionHandler.java     # @RestControllerAdvice (예외 → JSON)
│  └─ dto/                         # 요청/응답 스키마 (record)
│     ├─ HelloResponse.java
│     ├─ EchoRequest.java
│     ├─ EchoResponse.java
│     ├─ FieldValidationError.java
│     └─ ValidationErrorResponse.java
└─ config/
   └─ CorsConfig.java              # WebMvcConfigurer 로 /api/** CORS 설정
```

테스트는 `backend/src/test/java/com/godsse/backend` 아래에 같은 패키지 규칙으로 둡니다.

## 2. 클래스별 역할

| 클래스                     | 역할                                                                           |
| -------------------------- | ------------------------------------------------------------------------------ |
| `GodsseBackendApplication` | `@SpringBootApplication` 진입점. `SpringApplication.run(...)` 실행             |
| `HelloController`          | `/api/hello` 조회 2개 + `/api/hello/echo` 전송 1개 처리                        |
| `EchoRequest`              | 요청 DTO. `name` `@NotBlank`, `message` `@NotBlank` + `@Size(2~100)` 검증 규칙 |
| `EchoResponse`             | 응답 DTO. 이름, 앞뒤 공백 제거한 메시지, 길이                                  |
| `HelloResponse`            | 응답 DTO. 메시지 + `Instant` 시각                                              |
| `ValidationErrorResponse`  | 검증 실패 응답 구조(전체 메시지 + 필드별 오류 목록)                            |
| `FieldValidationError`     | 필드 단위 오류(`field`, `reason`)                                              |
| `ApiExceptionHandler`      | `MethodArgumentNotValidException` → 400 + 표준 JSON 으로 변환                  |
| `CorsConfig`               | `app.cors.allowed-origins` 값을 읽어 `/api/**` 에 CORS 허용                    |

### 컨트롤러 동작 예 (HelloController)

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

> 코드 스타일: Spring 관례대로 **탭 인덴트**, 생성자 주입, DTO 는 `record` 를 사용합니다. (Lombok 미사용)

## 3. 의존성 (`pom.xml`)

| 의존성                                | 범위    | 용도                                    |
| ------------------------------------- | ------- | --------------------------------------- |
| `spring-boot-starter-webmvc`          | compile | REST 컨트롤러, JSON(Jackson)            |
| `spring-boot-starter-validation`      | compile | `@Valid`, `@NotBlank`, `@Size`          |
| `spring-boot-starter-actuator`        | compile | `/actuator/health` 등 운영 엔드포인트   |
| `spring-boot-devtools`                | runtime | 개발 중 자동 재시작(배포 시 제외)       |
| `spring-boot-starter-webmvc-test`     | test    | MockMvc 등 웹 계층 테스트               |
| `spring-boot-starter-validation-test` | test    | 검증 관련 테스트 지원                   |
| `spring-boot-starter-actuator-test`   | test    | Actuator 테스트 지원                    |
| `spring-boot-maven-plugin`            | build   | `spring-boot:run`, 실행 가능 jar 패키징 |

- 부모: `org.springframework.boot:spring-boot-starter-parent:4.1.1`
- Java: `<java.version>17</java.version>`
- `spring-boot-starter-webmvc-test` 는 내부적으로 `spring-boot-starter-test`(JUnit 5, AssertJ, Mockito, json-path)를 함께 가져옵니다.

## 4. 설정 (`application.properties`)

| 프로퍼티                                    | 값                      | 설명                             |
| ------------------------------------------- | ----------------------- | -------------------------------- |
| `spring.application.name`                   | `godsse-backend`        | 애플리케이션 이름                |
| `server.port`                               | `8080`                  | HTTP 포트                        |
| `app.cors.allowed-origins`                  | `http://localhost:5173` | CORS 허용 오리진(콤마로 여러 개) |
| `management.endpoints.web.exposure.include` | `health,info`           | Actuator 노출 엔드포인트 제한    |

## 5. 테스트

| 파일                            | 내용                                                              |
| ------------------------------- | ----------------------------------------------------------------- |
| `GodsseBackendApplicationTests` | `@SpringBootTest` 컨텍스트가 정상 기동하는지 확인(`contextLoads`) |
| `HelloControllerTest`           | `@SpringBootTest` + `@AutoConfigureMockMvc` 로 HTTP 계층 검증     |

`HelloControllerTest` 의 4개 케이스:

1. `GET /api/hello` → 200, `message` / `timestamp` 존재
2. `GET /api/hello/godsse` → 200, 이름이 포함된 메시지
3. `POST /api/hello/echo` 정상 요청 → 200, 공백 제거 + 길이 반환
4. `POST /api/hello/echo` 검증 실패(`name` 공백) → 400, `errors[0].field == "name"`

프로젝트 루트(`godsse`)에서 실행합니다.

```powershell
cd backend
.\mvnw.cmd test
```

> Spring Boot 4 에서는 웹 테스트 애노테이션 패키지가 변경되었습니다.
> `@WebMvcTest`, `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure`
> (`MockMvc` 자체는 Spring Framework 의 `org.springframework.test.web.servlet`)

## 6. 새 API 추가하는 방법

1. **DTO 추가** — `api/dto` 에 `record` 로 요청/응답 정의 (검증은 `jakarta.validation.constraints.*`)
2. **컨트롤러 추가** — `api` 패키지에 `@RestController` + `@RequestMapping("/api/...")`
3. **로직 분리** — 로직이 커지면 `service` 패키지로 분리 (현재 샘플은 컨트롤러 내부 처리)
4. **테스트 추가** — `src/test/java/.../api` 에 `@SpringBootTest` + `@AutoConfigureMockMvc` 테스트
5. **문서 갱신** — [05-api.md](./05-api.md) 에 명세 추가

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

## 7. 실행/빌드 명령

| 목적      | 명령                                                                    |
| --------- | ----------------------------------------------------------------------- |
| 개발 실행 | `.\mvnw.cmd spring-boot:run`                                            |
| 테스트    | `.\mvnw.cmd test`                                                       |
| 빌드      | `.\mvnw.cmd clean package` → `target/godsse-backend-0.0.1-SNAPSHOT.jar` |
| jar 실행  | `java -jar target\godsse-backend-0.0.1-SNAPSHOT.jar`                    |

## 관련 문서

- 요청 흐름 → [02-architecture.md](./02-architecture.md)
- API 명세 → [05-api.md](./05-api.md)
- 실행 환경/트러블슈팅 → [06-development-guide.md](./06-development-guide.md)
