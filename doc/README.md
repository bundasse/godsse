# godsse 문서 (doc)

`godsse` 는 **React (Vite)** 프론트엔드와 **Java Spring Boot** 백엔드로 구성된 풀스택 프로젝트입니다.
이 폴더에는 프로젝트 구조와 동작 흐름을 파악하기 위한 문서를 모아 둡니다.

- 문서 최종 갱신: 2026-11-02
- 프론트엔드: React 19.3.0 + Vite 8.3.0 (JavaScript, ESLint 10 + Prettier 3.9)
- 백엔드: Java **17** + Spring Boot **4.1.1** (Maven Wrapper 포함)

## 최우선 원칙 (필독)

이 프로젝트의 **작업 대원칙 4가지**는 [00-principles.md](./00-principles.md) 에 정리되어 있습니다.
AI 도구는 아래 파일을 통해 작업 시작 시 자동으로 참고합니다.

- Cline: [`.clinerules/00-principles.md`](../.clinerules/00-principles.md)
- 그 외 도구(Cursor, Codex 등): [`AGENTS.md`](../AGENTS.md)

## 문서 목록

| 문서                                                 | 내용                                                                           | 이런 때 보세요                    |
| ---------------------------------------------------- | ------------------------------------------------------------------------------ | --------------------------------- |
| [00-principles.md](./00-principles.md)               | **작업 대원칙(최우선)** — 한국어 진행, 계획·승인 후 수정, 설명 방식, 학습 정리 | 작업 시작 전 / 규칙이 궁금할 때   |
| [01-overview.md](./01-overview.md)                   | 프로젝트 개요, 기술 스택, 전체 디렉터리 구조와 파일별 역할                     | 프로젝트가 처음일 때              |
| [02-architecture.md](./02-architecture.md)           | 전체 구성도, 요청 흐름(시퀀스), 프록시/CORS 동작                               | "요청이 어떻게 흘러가는가?"       |
| [03-backend.md](./03-backend.md)                     | 백엔드 패키지·클래스, 의존성, 설정, 테스트, API 추가 방법                      | 백엔드 코드를 고칠 때             |
| [04-frontend.md](./04-frontend.md)                   | 컴포넌트 구조, 상태와 데이터 로딩, 린트·포맷 설정                              | 프론트엔드 코드를 고칠 때         |
| [05-api.md](./05-api.md)                             | API 명세(요청/응답/에러 형식)와 호출 예시                                      | API 를 호출하거나 추가할 때       |
| [06-development-guide.md](./06-development-guide.md) | 실행 순서, 검증 명령, 트러블슈팅, 코드 규칙, 변경 이력                         | 실행이 안 되거나 규칙이 궁금할 때 |
| [07-domain-model.md](./07-domain-model.md)           | 도메인 모델(테이블·관계·스포일러 규칙)                                         | 데이터 구조를 확인할 때          |


## 30초 요약

1. **프론트엔드(5173)** 에서 `/api/**` 를 호출하면 Vite 개발 서버가 **백엔드(8080)** 로 프록시한다.
2. 백엔드는 Spring Boot 4 + JPA(H2 파일 DB)이고, 로그인은 **HttpSession(JSESSIONID)** + `@LoginUser` 리졸버로 처리한다.
3. 기능 — 회원·프로필(아바타) · 감상(공개 범위·스포일러·태그·좋아요·답장) · 팔로우 · 알림(SSE) · 세션 달력(함께한 사람·감상 요청·연간 통계) · 룰 자동완성.
4. 요청 검증 실패와 업무 오류는 `ApiExceptionHandler` 가 `{ message, errors[] }` 로 통일한다(400/401/403/404/409).
5. 백엔드 테스트 **107건** 통과 + 프론트 `npm run lint`/`npm run build` 로 확인한다. 명령은 [06-development-guide.md](./06-development-guide.md).

## 실행 명령 요약

프로젝트 루트(`godsse`)에서 실행합니다.

```powershell
# 백엔드 (http://localhost:8080)
cd backend
.\mvnw.cmd spring-boot:run

# 프론트엔드 (http://localhost:5173)
cd frontend
npm install
npm run dev
```

자세한 내용은 [06-development-guide.md](./06-development-guide.md) 를 참고하세요.
