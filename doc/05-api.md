# 05. API 명세

> **현재 상태 (2026-10-01)**: 인증(회원가입·로그인·로그아웃·내 정보)과 사용자(프로필 조회·검색·편집·이미지 업로드) API 가 구현되어 있습니다.
> 팔로우·리뷰(감상)·피드 API 는 다음 단계에서 이 문서에 추가합니다.
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

## 2. 인증 (`/api/auth`)

로그인 상태는 **서버 세션 + 쿠키(JSESSIONID)** 로 유지됩니다. 응답 본문에 토큰이 없고,
브라우저가 쿠키를 자동으로 주고받습니다. 프론트엔드는 `credentials: 'include'` 만 지정하면 됩니다.

| 항목 | 내용 |
| --- | --- |
| 쿠키 | `JSESSIONID` (HttpOnly, SameSite=Lax, 30분 유지) |
| 로그인 실패 | 401 — 핸들과 비밀번호 중 무엇이 틀렸는지는 알려 주지 않음 |
| 로그인 필요 API | 미로그인 시 401 (`@LoginUser` 가 판단) |

### 2.1 `POST /api/auth/signup` — 회원가입

성공하면 **곧바로 로그인 상태**가 되고 201 을 반환합니다.

| 필드 | 타입 | 필수 | 검증 규칙 |
| --- | --- | --- | --- |
| `handle` | string | Y | 영문·숫자·밑줄 3~20자, 중복 불가. 소문자로 저장 |
| `email` | string | Y | 이메일 형식, 100자 이하, 중복 불가. 소문자로 저장 |
| `password` | string | Y | 8~72자 |
| `nickname` | string | Y | 20자 이하, 중복 허용 |

```json
{ "handle": "GODSSE", "email": "GODSSE@example.com", "password": "password123", "nickname": "고드세" }
```

**응답 201**

```json
{
  "id": 1,
  "handle": "godsse",
  "nickname": "고드세",
  "bio": null,
  "profileImageUrl": null,
  "createdAt": "2026-09-25T10:01:30.323473Z"
}
```

**응답 400 (입력 규칙 위반)**

```json
{
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [{ "field": "handle", "reason": "핸들은 영문·숫자·밑줄 3~20자여야 합니다." }]
}
```

**응답 409 (중복)**

```json
{
  "message": "이미 사용 중인 핸들입니다.",
  "errors": [{ "field": "handle", "reason": "이미 사용 중인 핸들입니다." }]
}
```

### 2.2 `POST /api/auth/login` — 로그인

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| `handle` | string | Y |
| `password` | string | Y |

- **응답 200** — 프로필(회원가입과 같은 형태)
- **응답 401** — `{ "message": "핸들 또는 비밀번호가 올바르지 않습니다.", "errors": [] }`

### 2.3 `POST /api/auth/logout` — 로그아웃

세션을 버립니다. **응답 204** (본문 없음)

### 2.4 `GET /api/auth/me` — 내 정보

- **응답 200** — 프로필(회원가입과 같은 형태)
- **응답 401** — `{ "message": "로그인이 필요합니다.", "errors": [] }`

### 2.5 호출 예시 (PowerShell + curl.exe)

> PowerShell 에서 JSON 을 `-d` 로 넘기면 따옴표가 깨지기 쉽습니다.
> 본문을 파일로 저장하고 `--data-binary "@파일"` 로 보내는 방법이 안전합니다.

```powershell
# 본문을 UTF-8 파일로 저장
$body = '{"handle":"godsse","email":"godsse@example.com","password":"password123","nickname":"고드세"}'
[System.IO.File]::WriteAllText("$env:TEMP\signup.json", $body, (New-Object System.Text.UTF8Encoding($false)))

# 쿠키를 파일에 저장(-c)하고 다음 요청에서 재사용(-b)
curl.exe -c "$env:TEMP\ck.txt" -X POST http://localhost:8080/api/auth/signup `
  -H "Content-Type: application/json" --data-binary "@$env:TEMP\signup.json"

curl.exe -b "$env:TEMP\ck.txt" http://localhost:8080/api/auth/me
curl.exe -b "$env:TEMP\ck.txt" -X POST http://localhost:8080/api/auth/logout
```


## 3. 사용자 (`/api/users`)

### 3.1 `GET /api/users/{handle}` — 프로필 조회

로그인 없이 볼 수 있습니다.

**응답 200**

```json
{
  "id": 33,
  "handle": "godsse",
  "nickname": "고드세",
  "bio": "주 1회 크툴루의 부름을 돌립니다.",
  "profileImageUrl": "/uploads/profiles/1b80d4d9382f46099d7750289783fb9e.png",
  "createdAt": "2026-09-25T10:01:30.323473Z",
  "receivedReviewCount": 3,
  "writtenReviewCount": 5,
  "receivedLikeCount": 12,
  "receivedReplyCount": 4,
  "followerCount": 12,
  "followingCount": 8,
  "isFollowing": false,
  "badges": ["FIRST_REVIEW"]
}
```

| 필드 | 설명 |
| --- | --- |
| `profileImageUrl` | 이미지를 올리지 않았으면 `null` |
| `receivedReviewCount` | 이 사람이 받은 감상 수 |
| `writtenReviewCount` | 이 사람이 쓴 감상 수 |
| `receivedLikeCount` | 이 사람이 쓴 감상들이 받은 **하트 총합** |
| `receivedReplyCount` | 이 사람이 쓴 감상들에 달린 **답장 총합** |
| `followerCount` | 이 사람을 팔로우하는 사람 수 |
| `followingCount` | 이 사람이 팔로우하는 사람 수 |
| `isFollowing` | **조회자**가 이 사람을 팔로우 중인지. 로그인하지 않았다면 `false` |
| `badges` | 활동 배지 코드 목록(아래 표). 없으면 빈 배열 |

**배지 코드** — 서버는 코드만 주고, 화면 문구는 `frontend/src/constants/badges.js` 가 정합니다.

| 코드 | 조건 | 화면 문구 |
| --- | --- | --- |
| `FIRST_REVIEW` | 쓴 감상 ≥ 1 | 🖊️ 첫 감상 |
| `REVIEW_10` | 쓴 감상 ≥ 10 | 📚 감상 10편 |
| `RECEIVED_10` | 받은 감상 ≥ 10 | 💌 감상 10편 받음 |
| `HEARTS_10` | 받은 하트 ≥ 10 | ❤️ 하트 10개 |
| `FOLLOWERS_10` | 팔로워 ≥ 10 | 👥 팔로워 10명 |

> 배지는 DB 에 저장하지 않고 **조회할 때마다 숫자로 계산**합니다. 그래서 좋아요를 취소해 10개 아래로 내려가면 배지도 바로 사라집니다.

**응답 404** — 없는 핸들

```json
{
  "message": "해당 핸들의 사용자를 찾을 수 없습니다.",
  "errors": [{ "field": "handle", "reason": "해당 핸들의 사용자를 찾을 수 없습니다." }]
}
```

### 3.2 `GET /api/users?q=&page=&size=` — 유저 검색

로그인 없이 사용할 수 있습니다.

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `q` | (없음) | 핸들 또는 닉네임의 일부. 비어 있으면 빈 목록을 반환 |
| `page` | 0 | 페이지 번호(0부터 시작) |
| `size` | 20 | 한 페이지 개수 (서버에서 최대 50으로 제한) |

**응답 200**

```json
{
  "items": [{ "handle": "godsse", "nickname": "고드세", "profileImageUrl": null }],
  "page": 0,
  "size": 20,
  "total": 1,
  "totalPages": 1,
  "hasNext": false
}
```

### 3.3 `PATCH /api/users/me` — 프로필 편집 (로그인 필요)

핸들은 주소(`/u/핸들`)에 쓰이므로 바꿀 수 없습니다. 프로필 이미지는 3.4 로 올립니다.

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `nickname` | string | Y | 20자 이하 |
| `bio` | string | N | 300자 이하 (앞뒤 공백 제거, 비면 `null` 로 저장) |

- **응답 200** — 갱신된 `UserProfileResponse`
- **응답 400** — 닉네임이 비었거나 길이 초과
- **응답 401** — 미로그인

### 3.4 `POST /api/users/me/avatar` — 프로필 이미지 업로드 (로그인 필요)

`multipart/form-data` 로 보내며 필드 이름은 **`file`** 입니다. (JSON 이 아님)

| 제한 | 값 |
| --- | --- |
| 허용 형식 | PNG · JPEG · GIF · WEBP — Content-Type 과 확장자를 **둘 다** 확인 |
| 최대 크기 | 2MB (`app.upload.max-bytes`) |
| 저장 이름 | UUID (원래 파일 이름을 쓰지 않음 → 덮어쓰기·경로 조작·캐시 문제 방지) |
| 공개 경로 | `/uploads/profiles/{uuid}.{확장자}` |

- **응답 200** — 갱신된 `UserProfileResponse` (새 `profileImageUrl` 포함)
- **응답 400** — 이미지가 아니거나 크기 초과 (`errors[0].field = "file"`)
- **응답 401** — 미로그인

```powershell
# PowerShell 예시 (-F 는 multipart/form-data 로 전송)
curl.exe -b "$env:TEMP\ck.txt" -X POST http://localhost:8080/api/users/me/avatar `
  -F "file=@C:\path\to\photo.png;type=image/png"
```

> 올린 이미지는 `http://localhost:8080/uploads/profiles/{파일명}` 으로 바로 볼 수 있습니다.
> (백엔드 `WebConfig` 가 업로드 폴더를 `/uploads/**` URL 에 연결)

### 3.5 `POST /api/users/{handle}/follow` — 팔로우 (로그인 필요)

- **응답 204** — 본문 없음
- **응답 400** — 자기 자신을 팔로우 (`errors[0].field = "handle"`)
- **응답 401** — 미로그인
- **응답 404** — 없는 핸들
- **응답 409** — 이미 팔로우한 사용자

### 3.6 `DELETE /api/users/{handle}/follow` — 언팔로우 (로그인 필요)

- **응답 204** — 본문 없음
- **응답 401** — 미로그인
- **응답 404** — 팔로우하고 있지 않음

### 3.7 `GET /api/users/{handle}/followers` — 팔로워 목록

이 사람을 팔로우하는 사람들. 로그인 없이 볼 수 있습니다.

- **응답 200** — `PageResponse<UserSummary>` (최근 팔로우한 순)
- **응답 404** — 없는 핸들

### 3.8 `GET /api/users/{handle}/following` — 팔로잉 목록

이 사람이 팔로우하는 사람들. 응답 형태는 3.7 과 같습니다.

```json
{
  "items": [{ "handle": "godsse", "nickname": "고드세", "profileImageUrl": null }],
  "page": 0,
  "size": 20,
  "total": 1,
  "totalPages": 1,
  "hasNext": false
}
```

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `page` | 0 | 페이지 번호 |
| `size` | 20 | 한 페이지 개수 (서버에서 최대 50) |



## 4. 감상 (`/api/reviews`)

감상은 "누가 누구에게, 어느 세션(1회 플레이)에서 무엇을 느꼈는지"를 남기는 글입니다.
본문의 스포일러 마커는 **해석하지 않고 그대로 저장**하고, 화면에서 가립니다.

### 4.1 `POST /api/reviews` — 감상 작성 (로그인 필요)

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `toHandle` | string | Y | 감상을 받을 사람의 핸들 (자기 자신은 불가) |
| `playedOn` | date | Y | `2026-10-01` 형식, **오늘까지만** |
| `ruleName` | string | Y | 50자 이하 |
| `scenarioName` | string | N | 100자 이하 |
| `body` | string | Y | **100자 이상** 10000자 이하 |
| `spoilerMode` | string | N | `NONE` / `BLUR` / `FOLD` (기본 `NONE`) |
| `visibility` | string | N | `PUBLIC` / `FOLLOWERS` / `PRIVATE` (기본 `PUBLIC`) |
| `tags` | string[] | N | 최대 8개 (예: `["RP","HORROR"]`) |

**응답 201**

```json
{
  "id": 1,
  "author": { "handle": "writer", "nickname": "고드세", "profileImageUrl": null },
  "recipient": { "handle": "partner", "nickname": "파트너", "profileImageUrl": null },
  "playedOn": "2026-10-01",
  "ruleName": "크툴루의 부름 7판",
  "scenarioName": "밤의 미궁",
  "body": "오늘 정말 즐거웠습니다. ||범인은 집사였습니다.|| 다음에도 또 해요!",
  "spoilerMode": "NONE",
  "visibility": "PUBLIC",
  "tags": ["RP", "HORROR"],
  "createdAt": "2026-10-14T12:49:26.433822Z",
  "updatedAt": "2026-10-14T12:49:26.433822Z",
  "editable": true,
  "likeCount": 2,
  "liked": false,
  "replyCount": 1
}
```

- **응답 400** — 본문 100자 미만(`errors[0].field = "body"`), 날짜 누락·미래, 자기 자신
- **응답 401 / 404** — 미로그인 / 없는 핸들

### 4.2 `GET /api/reviews` — 감상 목록

로그인 없이도 볼 수 있습니다(공개 범위 안에서).

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `author` | (없음) | 이 사람이 **쓴** 감상 |
| `recipient` | (없음) | 이 사람이 **받은** 감상 |
| `following` | `false` | `true` 면 **내가 팔로우한 사람들의 감상**만 (로그인 필요, 미로그인이면 401) |
| `page` / `size` | 0 / 20 | 페이지 (size 최대 50) |

파라미터를 주지 않으면 **홈 피드**(전체 공개 감상, 최신순)입니다.

```json
{
  "items": [
    {
      "id": 1,
      "author": { "handle": "writer", "nickname": "고드세", "profileImageUrl": null },
      "recipient": { "handle": "partner", "nickname": "파트너", "profileImageUrl": null },
      "playedOn": "2026-10-01",
      "ruleName": "크툴루의 부름 7판",
      "scenarioName": "밤의 미궁",
      "bodyPreview": "오늘 정말 즐거웠습니다. …",
      "spoilerMode": "NONE",
      "tags": ["RP"],
      "createdAt": "2026-10-14T12:49:26.433823Z",
      "likeCount": 2,
      "liked": false
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1,
  "totalPages": 1,
  "hasNext": false
}
```

> 목록은 본문 전문 대신 **미리보기(`bodyPreview`)** 를 내려줍니다. 자를 때 스포일러 마커가 잘려
> 나가지 않도록 안전하게 자릅니다. ([study/06-review-spoiler.md](./study/06-review-spoiler.md) 참고)

### 4.3 `GET /api/reviews/{id}` — 감상 전문

- **응답 200** — `ReviewResponse` (수정·삭제 가능 여부 `editable`, 좋아요 `likeCount`·`liked`, 답장 수 `replyCount`)
- **응답 403** — 공개 범위 밖
- **응답 404** — 없는 감상

### 4.4 `PATCH /api/reviews/{id}` · `DELETE /api/reviews/{id}` — 수정·삭제 (작성자만)

수정 요청은 4.1 에서 `toHandle` 만 뺀 형태입니다(받는 사람은 바꿀 수 없음).

- 수정: **200** / **403**(작성자 아님) / **404**
- 삭제: **204** / **403** / **404**

### 4.5 `POST`·`DELETE /api/reviews/{id}/like` — 좋아요 (로그인 필요)

| 요청 | 응답 |
| --- | --- |
| `POST /api/reviews/{id}/like` | **204** (이미 눌렀으면 **409**) |
| `DELETE /api/reviews/{id}/like` | **204** (누르지 않았으면 **404**) |

- 감상을 **볼 수 없으면 403**, 미로그인이면 **401**
- 좋아요 수·내 좋아요 여부는 감상 응답의 `likeCount`·`liked` 로 내려간다

### 4.6 `GET`·`POST /api/reviews/{id}/replies` · `DELETE .../replies/{replyId}` — 답장

답장은 **한 단계**만 둡니다(답장에 다시 답장하는 구조는 없음).

| 요청 | 응답 |
| --- | --- |
| `GET /api/reviews/{id}/replies` | **200** — `ReviewReplyResponse[]` (오래된 순 = 대화 순서) |
| `POST /api/reviews/{id}/replies` | **201** — 작성된 답장 (로그인 필요) |
| `DELETE /api/reviews/{id}/replies/{replyId}` | **204** — 답장 작성자만 (아니면 **403**) |

- 요청 본문: `{ "body": "..." }` — **1~500자**
- 감상을 볼 수 없으면 조회·작성 모두 **403**

```json
[
  {
    "id": 3,
    "author": { "handle": "partner", "nickname": "파트너", "profileImageUrl": null },
    "body": "저도 정말 즐거웠어요!",
    "createdAt": "2026-10-15T10:20:00.000Z",
    "editable": false
  }
]
```

### 4.7 공개 범위와 스포일러 마커

| 공개 범위 | 볼 수 있는 사람 |
| --- | --- |
| `PUBLIC` | 누구나 (비로그인 포함) |
| `FOLLOWERS` | 작성자를 팔로우한 사람 + 작성자 + 받은 사람 |
| `PRIVATE` | 작성자와 받은 사람만 |

| 마커 | 쓰는 곳 | 화면 동작 |
| --- | --- | --- |
| `\|\|문장\|\|` | 문장 안 | 그 문장만 흐리게 가리고, 누르면 보인다 |
| `:::spoiler` | 단독 줄 | 그 지점 이후를 접고, "스포일러 펼치기"로 연다 |

## 5. 세션 일정 (`/api/sessions`)

마이페이지 **달력**에 기록하는 "내가 플레이할(또는 플레이한) 세션" 일정입니다.
감상과 달리 **나만 보는 계획표**이므로 모든 API 가 로그인을 요구합니다.

### 5.1 `GET /api/sessions?from=&to=` — 기간 내 내 일정

| 파라미터 | 필수 | 설명 |
| --- | --- | --- |
| `from` | Y | 시작일 (`YYYY-MM-DD`) |
| `to` | Y | 종료일 (`YYYY-MM-DD`) — `from` 이후, 최대 62일 |

```json
[
  {
    "id": 1,
    "title": "정기 캠페인 3화",
    "plannedDate": "2026-10-20",
    "ruleName": "크툴루의 부름 7판",
    "scenarioName": "밤의 미궁",
    "gm": true,
    "memo": "모집 중",
    "status": "PLANNED",
    "createdAt": "2026-10-15T09:12:00.123Z",
    "participants": [
      { "handle": "godssefan", "nickname": "고드세팬", "profileImageUrl": null }
    ]
  }
]
```

- **응답 400** — 기간이 잘못되었거나 62일 초과
- **응답 401** — 미로그인
- `participants` — 함께한 사람(5.5). 없으면 빈 배열

> 달력은 한 달치를 한 번에 그리므로 페이지네이션 대신 **기간**으로 받습니다.

### 5.2 `POST /api/sessions` — 일정 작성

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `title` | string | Y | 60자 이하 |
| `plannedDate` | date | Y | 미래·과거 모두 가능 |
| `ruleName` | string | Y | 50자 이하 |
| `scenarioName` | string | N | 100자 이하 |
| `gm` | boolean | N | 생략하면 `false` |
| `memo` | string | N | 1000자 이하 |
| `status` | string | N | 생략하면 `PLANNED` |

- **응답 201** — `SessionPlanResponse`
- **응답 400 / 401**

### 5.3 `PATCH /api/sessions/{id}` — 일정 수정 (소유자만)

5.2 와 같은 필드입니다. `status` 를 보내면 상태도 바뀝니다(예: `PLANNED` → `DONE`).
`gm`·`status` 를 생략하면 **기존 값을 유지**합니다.

- **응답 200 / 403**(남의 일정) / **404**

### 5.4 `DELETE /api/sessions/{id}` — 일정 삭제 (소유자만)

- **응답 204 / 403 / 404**

### 5.5 참가자(함께한 사람) — `POST`·`DELETE /api/sessions/{id}/participants` (소유자만)

세션을 마친 뒤 "누구에게 감상을 쓸지" 기억하지 않아도 되도록, 일정에 함께한 사람을 기록합니다.
**가입한 사용자만** 핸들로 추가할 수 있습니다(프로필·감상 작성 링크로 이어 주려면 실제 사용자여야 함).

```text
POST   /api/sessions/1/participants          {"handle":"godssefan"}   → 204
DELETE /api/sessions/1/participants/godssefan                        → 204
```

| 상황 | 코드 |
| --- | --- |
| 성공 | **204** |
| `handle` 이 비었거나 나 자신 | **400** |
| 없는 핸들 · 없는 일정 · 기록되지 않은 참가자 삭제 | **404** |
| 이미 참가자 | **409** |
| 남의 일정 | **403** |
| 미로그인 | **401** |

> 참가자는 목록 응답(`5.1`)의 `participants` 로 함께 내려옵니다. 일정을 삭제하면 참가자 행도 함께 지워집니다.

### 5.6 상태 값

| 값 | 뜻 |
| --- | --- |
| `PLANNED` | 예정 |
| `DONE` | 완료 |
| `CANCELED` | 취소 |

## 6. 알림 (`/api/notifications`)

알림은 다른 기능의 곁다리로 **서버가 만들어 준다**(감상·좋아요·답장·팔로우). 화면은 조회와 읽음 처리만 한다.
모든 요청은 **로그인 필요**(미로그인 **401**).

| 요청 | 응답 |
| --- | --- |
| `GET /api/notifications` | **200** — 최근 30건(최신순) + 안 읽은 개수 |
| `GET /api/notifications/unread-count` | **200** — `{ "count": 3 }` (헤더 배지) |
| `GET /api/notifications/stream` | **200** — 실시간 구독(SSE, `text/event-stream`) |
| `POST /api/notifications/{id}/read` | **204** (없거나 남의 알림이면 **404**) |
| `POST /api/notifications/read-all` | **204** |

```json
{
  "items": [
    {
      "id": 12,
      "type": "REPLY",
      "actor": { "handle": "partner", "nickname": "파트너", "profileImageUrl": null },
      "reviewId": 65,
      "read": false,
      "createdAt": "2026-10-15T10:20:00.000Z"
    },
    {
      "id": 11,
      "type": "FOLLOW",
      "actor": { "handle": "reader", "nickname": "리더", "profileImageUrl": null },
      "reviewId": null,
      "read": true,
      "createdAt": "2026-10-15T09:00:00.000Z"
    }
  ],
  "unreadCount": 1
}
```

### 6.1 알림이 생기는 순간

| 종류 | 언제 | 받는 사람 |
| --- | --- | --- |
| `REVIEW` | 감상이 작성됐을 때 | **감상을 받은 사람** |
| `LIKE` | 좋아요를 눌렀을 때 | 감상 작성자 |
| `REPLY` | 답장이 달렸을 때 | 감상 작성자 **와** 받은 사람 |
| `FOLLOW` | 팔로우했을 때 | 팔로우 당한 사람 |

- **내 활동은 알림을 만들지 않는다** — 내 감상에 내가 좋아요해도 알림이 생기지 않는다.
- **되돌린 행동은 알림도 지운다** — 좋아요 취소·언팔로우 시 해당 알림이 사라진다(사실과 다른 알림을 남기지 않는다).
- `FOLLOW` 알림은 `reviewId` 가 `null` 이고, 화면에서는 상대 프로필로 이동한다.

### 6.2 실시간 구독(SSE)

```text
GET /api/notifications/stream

event:connected
data:{"count":0}

event:changed
data:{"count":1}
```

| 이벤트 | 언제 | 데이터 |
| --- | --- | --- |
| `connected` | 연결 직후 1회 | `{"count": 현재 안 읽은 개수}` |
| `changed` | 개수가 바뀔 때(알림 생성·삭제·읽음) | `{"count": 새 개수}` |

- 서버는 **30분**마다 연결을 끊고, 브라우저의 `EventSource` 가 **자동으로 다시 연결**한다.
- 끊긴 연결은 서버가 목록에서 지운다(정상 종료·시간 초과·오류).
- 이 이벤트로 배지 숫자만 갱신하고, 알림 **목록**은 `GET /api/notifications` 로 받는다.
  (실시간 스트림은 "바뀌었다"는 신호이고, 내용의 원본은 DB 다)

## 7. `GET /api/hello`

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

## 8. `GET /api/hello/{name}`

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

## 9. `POST /api/hello/echo`

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

## 10. `GET /actuator/health`

```json
{ "status": "UP" }
```

- 노출 엔드포인트는 `application.properties` 의 `management.endpoints.web.exposure.include=health,info` 로 제한되어 있습니다.

## 11. 호출 예시

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

## 12. 상태 코드 정리

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
