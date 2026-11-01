/**
 * 룰(규칙 체계) 자동완성 기본 목록.
 *
 * 서버({@code GET /api/rules})에서 내려주는 목록이 있으면 그것을 우선 사용하고,
 * 아직 서버 데이터가 없을 때 이 목록으로 화면을 채운다. (룰은 자유 입력도 허용한다)
 *
 * `code` 는 약어(예: `COC7`), `label` 은 화면에 보이는 이름이다.
 * 약어만 쳐도 찾을 수 있게 하려고 두 값을 함께 둔다.
 */
export const FALLBACK_RULE_SYSTEMS = [
  { code: 'COC7', label: '크툴루의 부름 7판' },
  { code: 'COC6', label: '크툴루의 부름 6판' },
  { code: 'DND5E', label: '던전 앤 드래곤 5판' },
  { code: 'DW', label: '던전월드' },
  { code: 'INSANE', label: '인세인' },
  { code: 'MAGICA', label: '마기카로기아' },
  { code: 'SHINOBIGAMI', label: '시노비가미' },
  { code: 'PHANTOM', label: '팬텀 블러드' },
]
