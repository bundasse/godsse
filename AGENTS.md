# AGENTS.md — godsse 프로젝트 작업 지침

이 파일은 **`AGENTS.md` 를 지원하는 모든 AI 도구**(Cline, Cursor, Codex 등)가 참고하는 공통 지침입니다.
Cline 은 추가로 [`.clinerules/00-principles.md`](./.clinerules/00-principles.md) 를 자동으로 읽습니다.
두 문서의 내용은 동일하며, **충돌 시 대원칙 문서를 우선**합니다.

## 대원칙 (요약)

1. **한국어 우선** — 보고·질문·설명은 한국어로. 한국어로 정확한 설명이 불가능한 경우에만 영어로.
2. **계획 → 승인 → 실행** — 지시를 받으면 먼저 계획(요구사항 요약 / 변경 파일·내용 / 검증 방법 / 리스크·대안)을
   보고하고, **명시적 승인 후에만** 파일 생성·수정·삭제 및 상태 변경 명령을 실행. 읽기·검색은 예외로 승인 불필요.
3. **사용자 배경 고려** — 사용자는 Vue 프론트엔드 개발자이며 백엔드·Java·Spring·컴퓨터공학은 초급.
   프론트엔드(Vue/JS) 개념에 비유해 설명하고, 용어는 `한국어(영어 원어)` + 한 줄 정의로 소개.
4. **학습 자료화 + 읽기 요청** — 백엔드·Java·Spring·CS 내용은 `doc/study/` 에 학습 노트로 정리하고,
   정리 직후 "이 파일을 읽어보세요"라고 경로와 함께 명시적으로 요청.

> 상세 설명: [doc/00-principles.md](./doc/00-principles.md)

## 프로젝트 컨텍스트

- 구조: `frontend/`(React 19 + Vite 8, JavaScript) · `backend/`(Spring Boot 4.1.1 + Java 17, Maven Wrapper) ·
  `doc/`(문서) · `doc/study/`(학습 노트)
- 실행
  - 백엔드: `cd backend` → `.\mvnw.cmd spring-boot:run` (http://localhost:8080)
  - 프론트엔드: `cd frontend` → `npm install` → `npm run dev` (http://localhost:5173, `/api` → 8080 프록시)
- 검증: 프론트 `npm run lint` · `npm run build` / 백엔드 `.\mvnw.cmd test` (JDK 17 필요)
- 문서 시작점: [doc/README.md](./doc/README.md)

## 알려진 상태

- 백엔드는 JDK 17 미설치로 아직 컴파일·테스트를 실행하지 못했습니다. 자세한 내용은
  [doc/06-development-guide.md](./doc/06-development-guide.md) 의 "남은 작업 (TODO)" 참고.
