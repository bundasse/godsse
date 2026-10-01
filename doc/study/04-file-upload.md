# 04. 파일 업로드 (multipart · 경로 조작 방지 · 정적 제공)

> 이 노트를 읽고 나면: 프로필 이미지가 업로드되고 브라우저에서 보이기까지의 과정,
> 그리고 파일 업로드에서 꼭 지켜야 하는 안전장치를 설명할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **multipart/form-data** | 파일과 값을 함께 보내는 HTTP 본문 형식. JSON 으로는 파일을 보낼 수 없다 |
| **MultipartFile** | Spring 이 multipart 본문의 파일 부분을 받아 주는 타입 |
| **정적 리소스(static resource)** | 서버가 코드 실행 없이 그대로 내려주는 파일(이미지·CSS 등) |
| **경로 조작(path traversal)** | `../../` 같은 이름으로 서버의 다른 폴더를 건드리는 공격 |
| **UUID** | 겹치지 않는 임의의 식별자. 파일 이름을 새로 만들 때 쓴다 |
| **boundary** | multipart 본문에서 값과 값 사이를 나누는 구분 문자열 |

## 2. 왜 필요한가

JSON 요청은 `{"nickname":"고드세"}` 처럼 텍스트만 담습니다. 이미지(바이너리)는 담을 수 없어서
파일 전송용 형식(multipart)이 따로 있습니다.

업로드는 편하지만 위험도 함께 옵니다.

| 위험 | 예시 | 대비 |
| --- | --- | --- |
| 실행 파일 업로드 | `.jsp`, `.php` 를 올려 서버에서 실행 시도 | 허용 형식을 **화이트리스트**로 제한(이미지 4종) |
| 경로 조작 | 파일 이름을 `../../application.properties` 로 | **UUID 로 이름을 새로 만든다** |
| 덮어쓰기 | 같은 이름의 파일이 서로를 지움 | 위와 같음 |
| 용량 폭주 | 수 GB 파일 업로드 | 용량 제한(2MB) + 서버 설정 |
| 위장 | 확장자만 `.png`, 내용은 다른 파일 | Content-Type 과 확장자를 함께 확인 |

## 3. 이 프로젝트에서는

```
[브라우저] FormData(file)  ──POST /api/users/me/avatar──▶  UserController.uploadAvatar()
                                                             └─ UserService.changeProfileImage()
                                                                  ├─ FileStorageService.storeProfileImage()
                                                                  │    ├─ 형식·용량 검증
                                                                  │    ├─ UUID 이름 생성
                                                                  │    └─ uploads/profiles/ 에 저장
                                                                  ├─ user.updateProfile(... 새 URL ...)
                                                                  └─ FileStorageService.deleteProfileImage(이전 URL)

[브라우저] GET /uploads/profiles/xxxx.png  ──▶  WebConfig 의 리소스 핸들러가 파일을 그대로 응답
```

| 파일 / 설정 | 역할 |
| --- | --- |
| `service/FileStorageService.java` | 저장·검증·삭제 |
| `service/UserService.java` | 새 파일 저장 → 사용자 갱신 → 이전 파일 삭제 순서 관리 |
| `config/WebConfig.java` | `uploads` 폴더를 `/uploads/**` URL 로 연결 |
| `config/UploadConfig.java` | 업로드 폴더(`Path` 빈) 생성 |
| `common/exception/BadRequestException.java` | 검증 실패 → 400 |
| `application.properties` | `app.upload.dir`, `app.upload.max-bytes`, `spring.servlet.multipart.*` |
| `frontend/src/api/users.js` | `FormData` 로 업로드 |
| `frontend/src/api/client.js` | `postForm()` — Content-Type 을 붙이지 않는 요청 |

## 4. 예제 코드

### 4.1 서버 — 검증부터 저장까지

```java
public String storeProfileImage(MultipartFile file) {
	validate(file);                                    // 1) 형식·용량 검사 (아니면 400)

	String fileName = UUID.randomUUID().toString().replace("-", "") + "."
			+ extractExtension(file.getOriginalFilename());   // 2) 새 이름 만들기
	Path target = this.uploadDirectory.resolve("profiles").resolve(fileName);

	Files.createDirectories(target.getParent());
	try (InputStream input = file.getInputStream()) {
		Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);   // 3) 저장
	}

	return "/uploads/profiles/" + fileName;             // 4) 브라우저가 접근할 경로
}
```

### 4.2 정적 제공 — 파일을 URL 로 연결

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
	String location = this.uploadDirectory.toUri().toString();
	if (!location.endsWith("/")) {
		location = location + "/";      // 리소스 위치는 반드시 / 로 끝나야 한다
	}
	registry.addResourceHandler("/uploads/**").addResourceLocations(location);
}
```

### 4.3 프론트엔드 — Content-Type 을 붙이면 안 된다

```js
const formData = new FormData()
formData.append('file', file)

// ❌ fetch(url, { headers: { 'Content-Type': 'application/json' }, body: formData })
//    → 브라우저가 boundary 를 못 붙여 서버가 파일을 해석하지 못한다
// ✅ Content-Type 을 생략하면 브라우저가 multipart/form-data; boundary=... 를 직접 만든다
apiClient.postForm('/api/users/me/avatar', formData)
```

### 4.4 지우기 — 새 파일 저장이 끝난 뒤에

```java
String previousUrl = user.getProfileImageUrl();
String newUrl = this.fileStorageService.storeProfileImage(file);
user.updateProfile(user.getNickname(), user.getBio(), newUrl);
this.fileStorageService.deleteProfileImage(previousUrl);   // 실패해도 예외를 던지지 않는다
```

파일 삭제는 DB 트랜잭션으로 되돌릴 수 없습니다. 그래서 **새 파일을 먼저 저장**하고 지웁니다.
(반대로 하면 저장에 실패했을 때 이미지가 사라진다)

## 5. 저장 위치와 운영 전환

| 항목 | 개발(현재) | 운영(나중) |
| --- | --- | --- |
| 저장 위치 | 서버 로컬 `backend/uploads/` | S3·R2 같은 오브젝트 스토리지 |
| 제공 방법 | 백엔드가 직접 파일 응답 | CDN/스토리지 URL |
| 교체 방법 | — | `FileStorageService` 의 메서드 내용만 바꾸면 호출부는 그대로 |

> 로컬 파일 저장은 서버를 여러 대로 늘리면(로드밸런싱) 서로 다른 파일을 갖게 되는 한계가 있습니다.
> Phase 4(불특정 다수 개방) 전에 스토리지 분리를 검토합니다.

## 6. 확인 문제

1. 왜 원래 파일 이름(`my photo.png`)을 그대로 쓰지 않고 UUID 로 바꿀까요?
2. 확장자만 `.png` 인 텍스트 파일을 막으려면 무엇을 함께 확인해야 할까요?
3. 프론트에서 `FormData` 를 보낼 때 `Content-Type` 을 지정하면 왜 실패할까요?
4. 새 파일을 저장한 뒤에 이전 파일을 지우는 이유는 무엇일까요?

## 관련 문서

- API 명세(사용자) → [../05-api.md](../05-api.md)
- 계층 구조와 트랜잭션 → [02-spring-layers.md](./02-spring-layers.md)
- 도메인 모델(`profileImageUrl`) → [../07-domain-model.md](../07-domain-model.md)
