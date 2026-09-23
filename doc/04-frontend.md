# 04. 프론트엔드 (React + Vite)

## 1. 파일 구조와 역할

기준 경로: `frontend`

| 경로                           | 역할                                                            |
| ------------------------------ | --------------------------------------------------------------- |
| `index.html`                   | `<div id="root">` 와 `/src/main.jsx` 로드                       |
| `src/main.jsx`                 | `createRoot(...).render(<StrictMode><App/></StrictMode>)`       |
| `src/App.jsx`                  | 예제 화면: 서버 메시지 조회 + echo 전송 폼                      |
| `src/App.css`, `src/index.css` | 카드/결과표/폼 스타일, CSS 변수 기반 색상                       |
| `src/api/client.js`            | 공통 fetch 래퍼(JSON 직렬화, 에러 변환, `get/post/put/delete`)  |
| `src/api/greeting.js`          | `/api/hello` API 함수(`fetchHello`, `fetchHelloTo`, `sendEcho`) |
| `vite.config.js`               | 플러그인, `@` 별칭, 포트(5173), `/api` 프록시                   |
| `eslint.config.js`             | ESLint flat config                                              |
| `.prettierrc.json`             | `semi: false`, `singleQuote: true`, `printWidth: 100`           |
| `jsconfig.json`                | `@/*` → `./src/*` 경로 별칭(에디터 인식용)                      |
| `.env.example`                 | `VITE_API_BASE_URL` 사용 예시                                   |

## 2. 컴포넌트 구조와 상태

```
<App>
├─ 상태
│   ├─ hello, helloError, loading        # 조회 결과 / 에러 / 로딩
│   └─ name, message, echo, echoError    # 폼 입력과 전송 결과
├─ 효과
│   └─ useEffect : 최초 1회 fetchHello() 호출 (ignore 플래그로 중복 반영 방지)
└─ JSX
    ├─ 섹션 1: GET /api/hello  → message / timestamp 표시 + "다시 요청" 버튼
    └─ 섹션 2: POST /api/hello/echo → name/message 입력 폼 + 결과 표
```

| 상태         | 초기값                            | 갱신 지점                                       |
| ------------ | --------------------------------- | ----------------------------------------------- |
| `hello`      | `null`                            | `showHello(data)` (조회 성공)                   |
| `helloError` | `null`                            | `showHelloError(message)`                       |
| `loading`    | `true`                            | `showHello` / `showHelloError` / `handleReload` |
| `name`       | `'godsse'`                        | name 입력 `onChange`                            |
| `message`    | `'React + Spring Boot 연동 확인'` | message 입력 `onChange`                         |
| `echo`       | `null`                            | `handleSubmit` 성공 시                          |
| `echoError`  | `null`                            | `handleSubmit` 실패 시                          |

## 3. API 호출 계층

```js
// App.jsx
fetchHello().then((data) => {
  /* 화면 반영 */
})
sendEcho({ name, message }).catch((error) => {
  /* 에러 표시 */
})

// src/api/greeting.js
export const fetchHello = () => apiClient.get('/api/hello')
export const sendEcho = ({ name, message }) => apiClient.post('/api/hello/echo', { name, message })

// src/api/client.js
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
fetch(`${BASE_URL}${path}`, {
  headers: { 'Content-Type': 'application/json', ...headers },
  ...rest,
})
```

- 응답 본문은 `response.text()` 로 읽은 뒤 JSON 파싱(빈 응답이면 `null`).
- `response.ok === false` 이면 `data.message` 로 `Error` 를 던집니다 → 호출부 `catch` 에서 화면 표시.
- `BASE_URL` 이 비어 있으면 상대 경로(`/api/...`)로 호출되며, Vite 프록시를 타고 백엔드로 전달됩니다.
- 백엔드를 직접 호출하려면 `frontend/.env.local` 에 `VITE_API_BASE_URL=http://localhost:8080` 지정(이 경우 백엔드 CORS 설정 필요).

## 4. Vite 설정 (`vite.config.js`)

| 설정            | 값                               | 의미                                  |
| --------------- | -------------------------------- | ------------------------------------- |
| `plugins`       | `react()`                        | React Fast Refresh 지원               |
| `resolve.alias` | `'@' → ./src`                    | `@/api/greeting` 처럼 절대경로 import |
| `server.port`   | `5173`                           | 개발 서버 포트                        |
| `server.proxy`  | `/api` → `http://localhost:8080` | 개발 중 CORS 없이 백엔드 호출         |

## 5. 린트/포맷 설정

`eslint.config.js` (flat config):

| 항목               | 내용                                                   |
| ------------------ | ------------------------------------------------------ |
| 대상 파일          | `**/*.{js,mjs,jsx}`                                    |
| 제외               | `dist`, `dist-ssr`, `coverage`                         |
| 언어 옵션          | `globals.browser`, `ecmaVersion: 'latest'`, JSX 활성화 |
| 기본 규칙          | `js.configs.recommended`                               |
| React Hooks        | `reactHooks.configs.flat.recommended`                  |
| React Refresh      | `reactRefresh.configs.vite` (컴포넌트만 export 하도록) |
| Prettier 충돌 방지 | `eslint-config-prettier/flat` (포맷팅 규칙 비활성화)   |

> **알아둘 규칙**: `eslint-plugin-react-hooks` 7.x 의 `react-hooks/set-state-in-effect` 는 effect 본문에서 **동기적으로** `setState` 를 호출하면 오류를 냅니다.
> 그래서 `App.jsx` 는 `fetchHello().then(...)` 처럼 **비동기 콜백 안에서** 상태를 갱신하고, 재조회는 이벤트 핸들러(`handleReload`)에서 처리합니다.

스크립트(`package.json`):

| 명령              | 동작                     |
| ----------------- | ------------------------ |
| `npm run dev`     | 개발 서버(5173) + HMR    |
| `npm run build`   | 프로덕션 빌드 → `dist/`  |
| `npm run preview` | 빌드 결과 로컬 미리보기  |
| `npm run lint`    | `eslint . --fix --cache` |
| `npm run format`  | `prettier --write src/`  |

## 6. 새 화면/기능 추가 순서

1. **API 함수 추가** — `src/api/<도메인>.js` 에 `apiClient` 를 사용해 함수 추가
2. **컴포넌트 추가** — `src/components/` 생성 후 `App.jsx` 에 배치(라우팅이 필요하면 `react-router` 도입)
3. **상태 관리** — 화면 단위는 `useState`, 공유 상태가 늘면 Context/Zustand 등 검토
4. **데이터 로딩** — effect 에서 로딩 시 `set-state-in-effect` 규칙을 피하려면 `.then(callback)` 또는 이벤트 핸들러 사용
5. **문서 갱신** — [05-api.md](./05-api.md), 필요 시 [02-architecture.md](./02-architecture.md)

## 7. 스타일 규칙(요약)

- 파일/변수: 컴포넌트 `PascalCase`, 함수·변수 `camelCase`, 상수 `UPPER_SNAKE_CASE`
- 클래스명: `block__element`(`app__title`, `card__title`, `form__input`) + 변형(`text--error`, `text--muted`)
- 색상/간격은 `src/index.css` 의 CSS 변수(`--color-*`, `--radius`) 사용
- 문자열은 홑따옴표, 세미콜론 없음, 한 줄 100자(Prettier 설정과 동일)

## 관련 문서

- 요청 흐름 → [02-architecture.md](./02-architecture.md)
- API 명세 → [05-api.md](./05-api.md)
- 실행/트러블슈팅 → [06-development-guide.md](./06-development-guide.md)
