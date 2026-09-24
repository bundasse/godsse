# godsse 문서 (doc)

`godsse` 는 **React (Vite)** 프론트엔드와 **Java Spring Boot** 백엔드로 구성된 풀스택 프로젝트입니다.
이 폴더에는 프로젝트 구조와 동작 흐름을 파악하기 위한 문서를 모아 둡니다.

- 문서 최종 갱신: 2026-09-24
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
2. 백엔드의 샘플 API 는 `HelloController` 하나이고, 응답은 `record` DTO 로 내려간다.
3. 프론트엔드는 `src/api/client.js`(공통 fetch 래퍼) → `src/api/greeting.js`(API 함수) → `App.jsx`(화면) 순서로 사용한다.
4. 요청 검증 실패는 `ApiExceptionHandler` 가 400 + `{ message, errors[] }` 형태로 통일해서 돌려준다.
5. 백엔드에는 Maven Wrapper(`mvnw.cmd`)가 포함되어 있어 **Maven 설치 없이** 실행할 수 있다 (JDK 17 만 필요).

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
