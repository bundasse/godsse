# godsse-frontend

React 19 + Vite 기반 프론트엔드입니다.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [ESLint](https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint) + [Prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode)

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Compile and Minify for Production

```sh
npm run build
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

### Format with [Prettier](https://prettier.io/)

```sh
npm run format
```

## 백엔드 연동

- 개발 서버(`vite`)는 `/api` 요청을 `http://localhost:8080` (Spring Boot) 으로 프록시합니다.
- 백엔드 주소가 다르면 `.env.local` 파일에 `VITE_API_BASE_URL` 을 지정하세요. (예: `VITE_API_BASE_URL=http://localhost:8080`)
- API 호출 코드는 `src/api/` 아래에 모아 두었습니다. (`client.js` 공통 fetch 래퍼, `greeting.js` 샘플 API)

## 문서

프로젝트 전체 구조와 동작 흐름은 상위 폴더의 문서를 참고하세요.

- [../doc/README.md](../doc/README.md) — 문서 인덱스
- [../doc/04-frontend.md](../doc/04-frontend.md) — 프론트엔드 구조/상태/설정 상세
- [../doc/05-api.md](../doc/05-api.md) — API 명세
