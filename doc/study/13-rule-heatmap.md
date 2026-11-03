# 13. 연간 히트맵과 개인화 색 — GitHub 잔디 그래프

> 이 노트를 읽고 나면: 날짜 목록을 **요일 격자**로 바꾸는 계산, 사용자 설정의 **기본값을 저장하지 않고 계산**하는 이유,
> 색상 피커처럼 **이벤트가 쏟아지는 입력**을 저장하는 방법(디바운스·되돌림)을 설명할 수 있습니다.

## 1. 무엇을 만들었나

마이페이지 → 세션 일정 탭 → "세션 기록" 카드에 **올해의 룰 달력**을 붙였습니다.

```
월  ░░▓▓░░▓░░░▓ …
화  ░▓░░░░▓░░░░ …
…
```

- 열 1개 = 1주, 행 1개 = 요일(일~토). 칸 색 = 그날 한 **룰**.
- 한 날에 룰이 2개 이상이면 칸을 **대각선 줄무늬**로 나눕니다.
- 범례(룰 이름 + 횟수 + 색 견본)에서 색을 바꾸면 **바로 저장**됩니다.

관련 파일: `RuleHeatmap.jsx`, `api/ruleColors.js`, `SessionPlanService.heatmap`, `RuleColorService`, `common/RuleColors`.

## 2. 서버 — "날짜별 룰" 만들기

```java
Map<LocalDate, List<String>> ruleNamesByDate = new LinkedHashMap<>();
Map<LocalDate, Integer> countByDate = new HashMap<>();

for (SessionPlan plan : plans) {              // 이미 날짜 → id 순으로 읽은 목록
	LocalDate date = plan.getPlannedDate();
	List<String> names = ruleNamesByDate.computeIfAbsent(date, key -> new ArrayList<>());

	if (!names.contains(plan.getRuleName())) {  // 같은 날 같은 룰 2회 → 색은 1개면 충분
		names.add(plan.getRuleName());
	}

	countByDate.merge(date, 1, Integer::sum);
}
```

| 선택 | 이유 |
| --- | --- |
| `LinkedHashMap` | 일정 목록이 이미 날짜 오름차순이라 **정렬을 다시 하지 않아도** 된다 |
| 빈 날은 응답에서 제외 | 1년은 366칸. 빈 칸까지 보내면 응답만 커지고 화면은 어차피 채운다 |
| 룰 이름 중복 제거 | 칸 색은 룰 하나당 하나. "2회"는 `count` 로 따로 전달 |
| 월별로 12번 조회하지 않음 | 한 해 수십 건이라 한 번에 읽고 메모리에서 묶는 편이 단순하다 |

> 5.8 의 연간 통계와 같은 조회를 씁니다. 두 기능이 같은 데이터를 **각자 계산**하지만,
> 저장해 두면 일정을 고칠 때마다 갱신해야 하므로 지금은 "읽고 세기"가 더 단순합니다.

## 3. 기본 색 — 저장하지 않고 계산한다

| 방식 | 장점 | 단점 |
| --- | --- | --- |
| 모든 룰의 색을 DB 에 저장 | 조회가 단순 | 룰이 늘 때마다 행 추가, 기본색 규칙을 바꿀 수 없음 |
| **지정한 색만 저장 + 기본색 계산** | 행이 쌓이지 않고, 규칙을 바꿔도 저장 데이터가 낡지 않음 | 계산이 **결정적**이어야 함 |

```java
public static String defaultColor(String ruleName) {
	int hash = ruleName == null ? 0 : ruleName.hashCode();
	return PALETTE.get(Math.floorMod(hash, PALETTE.size()));
}
```

- `String.hashCode()` 는 **자바 명세에 값이 고정**되어 있어 서버를 다시 켜도 같은 색이 나옵니다.
- `%` 대신 `Math.floorMod` 를 쓰는 이유: 해시가 음수일 수 있는데, `%` 는 음수를 돌려줘 인덱스가 깨집니다.
- 팔레트는 10색(`#2563eb`, `#16a34a`, …)으로, 사용자가 고르지 않아도 서로 구분됩니다.

> "사용자가 고른 색"과 "기본색"을 구분해야 화면에서 **기본색으로 되돌리기** 버튼을 보여 줄 수 있습니다.
> 그래서 응답에 `custom: true/false` 를 함께 내려줍니다.

## 4. 저장 API — 업서트와 멱등

```http
PUT    /api/users/me/rule-colors   { "ruleName": "던전월드", "color": "#ff8800" }
DELETE /api/users/me/rule-colors?ruleName=던전월드
GET    /api/users/me/rule-colors
```

| 결정 | 이유 |
| --- | --- |
| `(user_id, rule_name)` 유니크 | 한 사람이 같은 룰에 색을 두 줄 가지면 어느 색을 쓸지 알 수 없다 |
| PUT 은 **업서트** | 화면은 "지금 이 룰의 색은 이 값"만 알면 된다. 새로 만드는지 바꾸는지 구분할 필요가 없다 |
| DELETE 는 **멱등** | 지울 색이 없어도 204. 같은 요청을 두 번 보내도 결과가 같다(오류 처리 단순) |
| 룰 이름은 **본문·쿼리** | 룰은 자유 입력이라 `/` 가 들어갈 수 있다. 경로 변수면 이름이 잘린다 |
| 색은 **소문자 정규화** | `#FF8800` 과 `#ff8800` 이 다른 값으로 쌓이지 않게 한다 |

```java
RuleColor saved = this.ruleColorRepository.findByUserIdAndRuleName(loginUser.getId(), ruleName)
	.map(existing -> {
		existing.changeColor(color);   // 변경 감지(dirty checking)로 UPDATE
		return existing;
	})
	.orElseGet(() -> this.ruleColorRepository.save(new RuleColor(owner, ruleName, color)));
```

## 5. 화면 — 날짜 목록을 격자로

`buildYearGrid(year)` 는 **1월 1일 앞**과 **12월 31일 뒤**를 `null` 로 채워 모든 열이 7칸이 되게 합니다.

```jsx
for (let index = 0; index < first.getDay(); index += 1) {
  cells.push(null)                                 // 1월 1일이 목요일이면 앞 4칸
}

for (const date = new Date(first); date <= last; date.setDate(date.getDate() + 1)) {
  cells.push({ date: toIsoDate(date), month: date.getMonth(), day: date.getDate() })
}

while (cells.length % 7 !== 0) {
  cells.push(null)                                 // 마지막 주도 7칸으로
}
```

- 월 라벨은 "그 달 1일이 들어 있는 열"을 찾아 붙입니다(`findIndex`). 라벨과 칸을 같은 격자에 두어
  어긋나지 않습니다.
- `Date` 는 **로컬 시간** 기준으로만 더합니다(`setDate(+1)`). 날짜만 다루므로 시간대 변환이 끼면 오히려 하루씩 밀립니다.
- 칸 색은 인라인 스타일로 줍니다.

```jsx
const step = 100 / colors.length
const stops = colors.map((color, index) => `${color} ${index * step}% ${(index + 1) * step}%`)
return { backgroundImage: `linear-gradient(135deg, ${stops.join(', ')})` }
```

## 6. 색상 피커 — 이벤트가 쏟아지는 입력 다루기

`<input type="color">` 는 피커를 끄는 동안 `onChange` 가 **연속으로** 발생합니다.
그때마다 저장하면 요청이 수십 개 나가고, 응답 순서가 뒤바뀌면 **옛 색이 마지막에 저장**될 수도 있습니다.

그래서 화면은 세 단계로 나눴습니다.

1. **즉시 미리보기** — `drafts` 상태에 넣어 칸·견본을 바로 칠한다.
2. **500ms 디바운스** — 룰별 타이머(`Map`)를 두고 마지막 변경만 저장한다.
3. **실패하면 되돌림** — `drafts` 에서 지워 서버 값(응답의 `color`)으로 돌아가고 오류 문구를 보여 준다.

```jsx
clearTimeout(timers.current.get(ruleName))
timers.current.set(ruleName, setTimeout(() => {
  timers.current.delete(ruleName)

  saveRuleColor(ruleName, color)
    .then((saved) => setDrafts((previous) => ({ ...previous, [ruleName]: saved.color })))
    .catch((apiError) => {
      setDrafts((previous) => { const next = { ...previous }; delete next[ruleName]; return next })
      setSaveError(`'${ruleName}' 색을 저장하지 못했습니다. ${apiError.message}`)
    })
}, 500))
```

**초기화 버튼**에서 주의할 점: 대기 중인 저장을 먼저 취소합니다.
삭제한 뒤에 옛 저장 요청이 도착하면 색이 되살아나기 때문입니다.

```jsx
clearTimeout(timers.current.get(ruleName))   // ① 대기 취소
timers.current.delete(ruleName)

await resetRuleColor(ruleName)               // ② 삭제(204)
// ③ 기본색은 서버가 계산하므로 다시 받아 온다
setReloadCount((previous) => previous + 1)
```

> 남은 한계: 룰 하나에 대해 저장 요청이 **겹칠** 경우(디바운스 뒤에도 두 번 나가는 경우) 마지막 응답이
> 항상 최신 색이라는 보장은 없습니다. 색 개인화는 되돌리기 쉬운 설정이라 지금은 허용했고,
> 필요하면 `AbortController` 로 이전 요청을 취소하거나 요청 순번을 비교해 최신 것만 반영하면 됩니다.

## 7. 테스트로 확인한 것

| 테스트 | 확인 내용 |
| --- | --- |
| `common/RuleColorsTest` (5) | 팔레트가 모두 `#RRGGBB`, **같은 이름 → 같은 색**, 이름이 없어도 예외 없음, 형식 검증 |
| `api/RuleColorControllerTest` (8) | 저장 후 목록 반영, 두 번 저장해도 **한 줄**, 공백 다듬기, 삭제 **멱등**, 사용자별 분리, 400(`ruleName`·`color`), 401 |
| `api/SessionPlanControllerTest` (히트맵 5) | 날짜별 룰 묶음·중복 제거·횟수, 기본색/지정색(`custom`), **내 일정만**, 연도 400, 401 |

히트맵 테스트에서 `2026-11-01` 에 CoC 1건 + 던전월드 2건을 넣고,
`days[0].count = 3`, `days[0].ruleNames.length = 2`, `rules[0] = 던전월드 3회` 를 확인합니다.

## 8. 스스로 점검

1. 1월 1일이 금요일이면 첫 열의 앞 칸은 몇 개인가? (`getDay()` 값과 같은 수)
2. 기본색을 저장하지 않으면 서버를 바꿔도 사용자 색이 유지되는 이유는?
3. 색상 피커 이벤트를 그대로 저장하면 어떤 문제가 생기는가? (요청 폭주 + 응답 순서 역전)
4. "기본색으로" 버튼에서 타이머를 먼저 지우지 않으면 어떤 일이 생기는가?
5. 히트맵 응답에서 빈 날을 빼도 화면이 그릴 수 있는 이유는?

## 관련 문서

- API 명세 → [../05-api.md](../05-api.md) (3.9 룰 색, 5.9 히트맵)
- 테이블 구조 → [../07-domain-model.md](../07-domain-model.md) (3.11 `rule_colors`)
- 달력 날짜 다루기 → [07-session-calendar.md](./07-session-calendar.md)


