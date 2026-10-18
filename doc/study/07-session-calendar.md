# 07. 세션 일정 달력 (날짜 다루기)

> 이 노트를 읽고 나면: 날짜만 다룰 때 무엇을 조심해야 하는지(시간대 함정),
> 달력 칸을 어떻게 계산하는지, 기간 조회 API 를 왜 그렇게 설계했는지 설명할 수 있습니다.

## 1. 개념

| 용어 | 한 줄 정의 |
| --- | --- |
| **LocalDate** | "날짜만" 있는 값(`2026-10-20`). 시간·시간대가 없다 |
| **Instant** | "지금 이 순간"(UTC 기준 시각). 생성 시각 기록에 쓴다 |
| **시간대(timezone)** | 같은 순간을 다르게 표시하는 규칙(예: 한국 UTC+9) |
| **UTC** | 세계 표준시. `2026-10-20T00:00Z` 처럼 `Z` 가 붙는다 |
| **달력 칸(grid)** | 1일이 무슨 요일인지, 그 달이 며칠인지로 만드는 7칸 × 주 단위 표 |

이 프로젝트에서 역할이 나뉩니다.

| 값 | 쓰는 곳 |
| --- | --- |
| `LocalDate` | 세션 날짜, 플레이 날짜 (달력에 그리는 값) |
| `Instant` | `createdAt`, `updatedAt` (언제 저장했는지) |

## 2. 왜 조심해야 하는가 — 시간대 함정

자바스크립트의 `Date` 는 "날짜"가 아니라 "순간"입니다. 그래서 날짜만 다룰 때 사고가 납니다.

```js
// ❌ 문자열을 그대로 넣으면 UTC 자정으로 해석된다
new Date('2026-10-20')                      // 2026-10-20T00:00:00Z
                                            // → UTC-5 지역에서는 10월 19일 19시로 보인다

// ❌ toISOString() 은 UTC 로 바꿔서 문자열을 만든다
new Date(2026, 9, 20).toISOString()          // 한국 시간 10월 20일 00:00 → '2026-10-19T15:00:00Z'
                                             // → slice(0,10) 하면 '2026-10-19' (하루 전날!)
```

**안전한 방법**: 연·월·일을 직접 꺼내고 직접 만든다.

```js
function toIsoDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`   // 로컬 기준 'YYYY-MM-DD'
}

const date = new Date(2026, 9, 20)   // 월은 0부터! (9 = 10월)
toIsoDate(date)                       // '2026-10-20'
```

> 서버(자바)의 `LocalDate` 는 시간대가 없어서 안전합니다. **함정은 브라우저 쪽 변환**에서 생깁니다.

## 3. 이 프로젝트에서는

```
MyCalendar (브라우저)
  ├─ monthRange(year, month) → from·to ('YYYY-MM-DD')
  ├─ GET /api/sessions?from=2026-10-01&to=2026-10-31
  │     └─ SessionPlanService.list() (기간 검증: 최대 62일)
  │          └─ SessionPlanRepository.findByOwnerIdAndPlannedDateBetween...(LocalDate)
  └─ buildWeeks(year, month) → 달력 칸 + 날짜별 점 표시
```

| 파일 | 역할 |
| --- | --- |
| `domain/SessionPlan.java` | 날짜는 `LocalDate plannedDate` |
| `service/SessionPlanService.java` | 기간 검증(최대 62일), 소유자 확인(403) |
| `repository/SessionPlanRepository.java` | `Between` + 날짜·id 정렬 목록 |
| `frontend/components/MyCalendar.jsx` | 달력 칸 계산, 월 이동, 날짜 선택 |
| `frontend/components/SessionForm.jsx` | 일정 작성·수정(상태 변경 포함) |

### 3.1 달력 칸 계산 — 라이브러리 없이

```js
function buildWeeks(year, month) {
  const cells = []
  const firstWeekday = new Date(year, month, 1).getDay()      // 1일이 무슨 요일(0=일)
  const lastDay = new Date(year, month + 1, 0).getDate()      // 다음 달 0일 = 이번 달 말일

  for (let index = 0; index < firstWeekday; index += 1) cells.push(null)   // 앞 빈 칸
  for (let day = 1; day <= lastDay; day += 1) cells.push(day)              // 1~말일
  while (cells.length % 7 !== 0) cells.push(null)                          // 뒤 빈 칸 채우기

  const weeks = []
  for (let index = 0; index < cells.length; index += 7) weeks.push(cells.slice(index, index + 7))
  return weeks
}
```

`new Date(year, month + 1, 0)` 이 "그 달 마지막 날"이 되는 것은 자바스크립트 Date 의 오래된 규칙입니다.
(다음 달 0일 = 이번 달 말일)

### 3.2 목록을 기간으로 받는 이유

감상 목록은 페이지(`page`·`size`)로 받지만, **달력은 기간**으로 받습니다.

| 목록 | 조회 방식 | 이유 |
| --- | --- | --- |
| 감상 | 페이지네이션 | 무한히 쌓이고, 스크롤로 더 본다 |
| 세션 일정 | 기간(`from`~`to`) | 한 달치를 한 번에 그려야 달력 모양이 맞는다 |

기간을 제한하지 않으면 `from=2000-01-01&to=2100-12-31` 같은 요청이 들어올 수 있어 **최대 62일**로 막습니다.

## 4. 이번에 만난 함정 (기록)

| 증상 | 원인 | 해결 |
| --- | --- | --- |
| 선택 항목 `gm` 을 생략하면 400 `요청 본문을 읽을 수 없습니다` | Jackson 이 record 의 **primitive `boolean`** 파라미터를 채우지 못함 | DTO 를 `Boolean gm`(래퍼)으로 바꾸고 서비스에서 `Boolean.TRUE.equals(...)` 로 기본값 처리 |
| 수정 시 `gm` 을 생략하면 GM 여부가 사라짐 | 생략을 `false` 로 해석 | 수정에서는 `request.gm() == null ? plan.isGm() : request.gm()` 로 **기존 값 유지** |

## 5. 확인 문제

1. `LocalDate` 와 `Instant` 는 각각 어떤 값에 쓰면 좋을까요?
2. `new Date(2026, 9, 20).toISOString().slice(0, 10)` 이 한국에서 어떤 값을 돌려줄까요? 왜 그럴까요?
3. `new Date(year, month + 1, 0)` 은 무엇을 뜻할까요?
4. 달력 목록을 페이지네이션 대신 기간으로 받는 이유는 무엇일까요?
5. 요청 DTO 에서 primitive 대신 래퍼(`Boolean`)를 쓰면 무엇이 좋아질까요?

## 관련 문서

- API 명세(세션 일정) → [../05-api.md](../05-api.md)
- 도메인 모델(`session_plans`) → [../07-domain-model.md](../07-domain-model.md)
- 계층 구조와 트랜잭션 → [02-spring-layers.md](./02-spring-layers.md)
** 여기까지 읽음 **