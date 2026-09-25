# 04. 프론트엔드 (React + Vite)

기준 경로: `frontend`

## 1. 파일 구조와 역할

| 경로 | 역할 |
| --- | --- |
| `index.html` | `<div id="root">` 와 `/src/main.jsx` 로드 |
| `src/main.jsx` | `BrowserRouter` 안에 `<App />` 을 마운트 |
| `src/App.jsx` | **라우트 표**. 어떤 URL 에 어떤 페이지를 보여 줄지 정의 |
| `src/layouts/AppLayout.jsx` | 공통 뼈대(헤더 + 본문 + 모바일 하단 탭), `<Outlet />` 자리에 페이지가 들어감 |
| `src/layouts/AppLayout.css` | 헤더/하단 탭/본문 폭, 반응형 규칙 |
| `src/components/` | 재사용 컴포넌트 (Header, NavBar, PagePlaceholder, ProtectedRoute) |
| `src/context/auth.js` | 로그인 상태 컨텍스트 정의 + `useAuth()` 훅 |
| `src/context/AuthProvider.jsx` | 로그인 상태를 앱에 공급(시작 시 `/api/auth/me` 로 복원) |
| `src/pages/` | 화면 단위 컴포넌트 (라우트 1개 = 파일 1개) |
| `src/api/client.js` | 공통 fetch 래퍼(JSON 직렬화, 에러 변환, `get/post/put/delete`) |
| `src/api/greeting.js` | `/api/hello` 샘플 API 함수 (연결 확인용) |
| `src/api/auth.js` | 회원가입·로그인·로그아웃·내 정보 API 함수 |
| `src/constants/` | 태그·룰·스포일러 마커처럼 화면과 서버가 공유하는 상수 |
| `src/App.css` | 페이지/카드/폼/버튼 등 공용 클래스 |
| `src/index.css` | CSS 변수(색상·치수)·기본 리셋·폰트 |
| `vite.config.js` | 플러그인, `@` 별칭, 포트(5173), `/api` 프록시 |
| `eslint.config.js` | ESLint flat config |
| `.prettierrc.json` | `semi: false`, `singleQuote: true`, `printWidth: 100` |
| `jsconfig.json` | `@/*` → `./src/*` 경로 별칭(에디터 인식용) |

## 2. 라우팅

`react-router` **7.18.4** 를 사용합니다. (Vue Router 의 `routes` 배열과 같은 개념)

```jsx
// src/App.jsx
<Routes>
  <Route element={<AppLayout />}>
    <Route index element={<HomePage />} />                        {/* /            */}
    <Route path="login" element={<LoginPage />} />                {/* /login       */}
    <Route path="u/:handle" element={<ProfilePage />} />           {/* /u/godsse    */}
    <Route path="reviews/:id" element={<ReviewDetailPage />} />    {/* /reviews/12  */}
    <Route path="*" element={<NotFoundPage />} />                  {/* 그 외 전부    */}
  </Route>
</Routes>
```

| URL | 페이지 | 상태 |
| --- | --- | --- |
| `/` | `HomePage` | 뼈대 |
| `/login`, `/signup` | `LoginPage`, `SignupPage` | 뼈대 (다음 단계에서 폼 연결) |
| `/search` | `SearchPage` | 뼈대 |
| `/u/:handle` | `ProfilePage` | 뼈대 |
| `/u/:handle/received`, `/written` | 받은/쓴 감상 목록 | 뼈대 |
| `/reviews/new`, `/reviews/:id` | 감상 작성 / 전문 | 뼈대 |
| `/me`, `/me/edit` | 마이페이지 / 프로필 편집 | 뼈대 |

- URL 파라미터는 `useParams()`(예: `:handle`), 쿼리 문자열은 `useSearchParams()`(예: `?to=godsse`)로 읽습니다.
- `NavLink` 는 현재 경로와 같을 때 자동으로 활성 스타일 클래스를 붙여 줍니다.

> ⚠️ `react-router` 8.x 는 Node 22.22 이상이 필요합니다. 현재 개발 PC 는 Node 22.19 이므로 **7.x 를 유지**합니다.

## 3. 레이아웃과 반응형

```
AppLayout
├─ Header   (상단, 스크롤해도 따라옴)  — 로고 + PC 메뉴
├─ <main>   (본문, 최대 폭 --content-width = 720px)
└─ NavBar   (모바일 하단 탭)
```

| 화면 폭 | 동작 |
| --- | --- |
| ~767px (모바일) | 헤더 메뉴 숨김, 하단 탭 표시, 본문 아래 여백 확보 |
| 768px~ (PC) | 하단 탭 숨김, 헤더 메뉴 표시 |

치수는 `src/index.css` 의 CSS 변수(`--content-width`, `--header-height`, `--nav-height`)로 관리합니다.

## 4. API 호출 계층

```js
// 페이지 컴포넌트
fetchHello().then((data) => {
  /* 화면 반영 */
})

// src/api/greeting.js  — 도메인별 API 함수
export const fetchHello = () => apiClient.get('/api/hello')

// src/api/client.js    — 공통 fetch 래퍼
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
fetch(`${BASE_URL}${path}`, { headers: { 'Content-Type': 'application/json', ...headers }, ...rest })
```

- 응답 본문은 `response.text()` 로 읽은 뒤 JSON 파싱(빈 응답이면 `null`).
- `response.ok === false` 이면 `data.message` 로 `Error` 를 던집니다 → 호출부 `catch` 에서 화면 표시.
- `BASE_URL` 이 비어 있으면 상대 경로(`/api/...`)로 호출되며, Vite 프록시를 타고 백엔드로 전달됩니다.
- 백엔드를 직접 호출하려면 `frontend/.env.local` 에 `VITE_API_BASE_URL=http://localhost:8080` 지정(이 경우 백엔드 CORS 설정 필요).

## 5. Vite 설정 (`vite.config.js`)

| 설정 | 값 | 의미 |
| --- | --- | --- |
| `plugins` | `react()` | React Fast Refresh 지원 |
| `resolve.alias` | `'@' → ./src` | `@/api/greeting` 처럼 절대경로 import |
| `server.port` | `5173` | 개발 서버 포트 |
| `server.proxy` | `/api` → `http://localhost:8080` | 개발 중 CORS 없이 백엔드 호출 |

## 6. 린트/포맷 설정

`eslint.config.js` (flat config):

| 항목 | 내용 |
| --- | --- |
| 대상 파일 | `**/*.{js,mjs,jsx}` |
| 제외 | `dist`, `dist-ssr`, `coverage` |
| 언어 옵션 | `globals.browser`, `ecmaVersion: 'latest'`, JSX 활성화 |
| 기본 규칙 | `js.configs.recommended` |
| React Hooks | `reactHooks.configs.flat.recommended` |
| React Refresh | `reactRefresh.configs.vite` (컴포넌트만 export 하도록) |
| Prettier 충돌 방지 | `eslint-config-prettier/flat` (포맷팅 규칙 비활성화) |

> **알아둘 규칙 1**: `react-hooks/set-state-in-effect` 는 effect 본문에서 **동기적으로** `setState` 를 호출하면 오류를 냅니다.
> 데이터 로딩은 `.then(callback)` 또는 이벤트 핸들러에서 처리합니다.
>
> **알아둘 규칙 2**: `react-refresh/only-export-components` 때문에 컴포넌트 파일에서는 컴포넌트만 export 합니다.
> 상수는 `src/constants/*.js` 로 분리합니다.

스크립트(`package.json`):

| 명령 | 동작 |
| --- | --- |
| `npm run dev` | 개발 서버(5173) + HMR |
| `npm run build` | 프로덕션 빌드 → `dist/` |
| `npm run preview` | 빌드 결과 로컬 미리보기 |
| `npm run lint` | `eslint . --fix --cache` |
| `npm run format` | `prettier --write src/` |

## 7. 상태 관리 방침

| 범위 | 방법 |
| --- | --- |
| 화면 안의 상태 (입력값, 로딩, 오류) | `useState` |
| 로그인한 사용자(여러 화면 공유) | `AuthContext` — `src/context/auth.js` + `AuthProvider.jsx` |
| 그 외 전역 상태 | 필요해지면 Zustand 등 검토 (지금은 도입하지 않음) |

### 로그인 상태 흐름

| 파일 | 역할 |
| --- | --- |
| `src/context/auth.js` | `AuthContext` 와 `useAuth()` 훅 |
| `src/context/AuthProvider.jsx` | `user`, `loading`, `login`, `signup`, `logout` 을 공급 |
| `src/components/ProtectedRoute.jsx` | 로그인 필요한 화면을 감싼다(비로그인 → `/login`) |
| `src/api/auth.js` | `/api/auth/*` 호출 함수 |
| `src/api/client.js` | `credentials: 'include'`(쿠키 전송), `ApiError`(status·errors 포함) |

1. 앱이 시작되면 `AuthProvider` 가 `GET /api/auth/me` 로 **로그인 상태를 복원**한다.
   (세션 쿠키가 남아 있으면 200, 없으면 401 → 비로그인 상태로 처리)
2. 로그인/회원가입에 성공하면 `user` 가 채워지고 헤더·하단 탭이 즉시 바뀐다.
3. 로그아웃은 `POST /api/auth/logout` 후 `user` 를 비운다.
4. `/me`, `/me/edit`, `/reviews/new` 는 `ProtectedRoute` 로 감싸여 있고,
   비로그인 상태에서 접근하면 `/login` 으로 보낸 뒤 로그인 후 원래 주소로 돌아온다.

> **파일을 나눈 이유**: ESLint 의 `react-refresh/only-export-components` 규칙이
> "컴포넌트 파일에서는 컴포넌트만 export" 하도록 요구합니다. 그래서 컨텍스트 객체와 훅은
> `auth.js`(컴포넌트 없음)에, 컴포넌트는 `AuthProvider.jsx` 에 둡니다.

폼 오류는 `ApiError` 로 받아 전체 메시지와 입력칸별 메시지를 함께 표시한다.

```js
try {
  await signup(form)
} catch (apiError) {
  setError(apiError.message) // 전체 메시지
  setFieldErrors(
    Object.fromEntries((apiError.errors ?? []).map((item) => [item.field, item.reason])),
  )
}
```

## 8. 새 화면/기능 추가 순서

1. **API 함수 추가** — `src/api/<도메인>.js` 에 `apiClient` 를 사용해 함수 추가
2. **페이지 추가** — `src/pages/<이름>Page.jsx` 작성 → `src/App.jsx` 라우트 표에 등록
3. **공용 컴포넌트 분리** — 두 곳 이상에서 쓰면 `src/components/` 로 이동
4. **데이터 로딩** — effect 안 동기 `setState` 를 피한다(`.then(callback)` / 이벤트 핸들러)
5. **스타일** — 공용은 `src/App.css`, 레이아웃은 `layouts/AppLayout.css`, 화면 전용은 해당 페이지 옆 CSS
6. **문서 갱신** — [05-api.md](./05-api.md), 필요 시 이 문서와 [02-architecture.md](./02-architecture.md)

## 9. 스타일 규칙(요약)

- 파일/변수: 컴포넌트 `PascalCase`, 함수·변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`
- 클래스명: `block__element`(`page__title`, `card__title`, `form__input`) + 변형(`text--error`, `badge`)
- 색상/간격은 `src/index.css` 의 CSS 변수(`--color-*`, `--radius`, `--content-width`) 사용
- 문자열은 홑따옴표, 세미콜론 없음, 한 줄 100자(Prettier 설정과 동일)

## 관련 문서

- 요청 흐름 → [02-architecture.md](./02-architecture.md)
- API 명세 → [05-api.md](./05-api.md)
- 백엔드 상세 → [03-backend.md](./03-backend.md)
- 실행/트러블슈팅 → [06-development-guide.md](./06-development-guide.md)
