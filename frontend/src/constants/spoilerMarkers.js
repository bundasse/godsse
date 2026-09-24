/**
 * 본문에 쓰는 스포일러 마커.
 *
 * 백엔드는 마커를 해석하지 않고 문자열 그대로 저장한다. 해석(가리기/접기)은 화면에서 한다.
 * - 일부 가리기: `||스포일러 문장||` → 흐리게 가리고 클릭하면 보여 준다.
 * - 이후 접기: 단독 줄로 `:::spoiler` 를 쓰면 그 지점 이후가 접힌다.
 */
export const BLUR_MARKER = '||'

/** 이 줄만 있는 줄(앞뒤 공백 제외)이 나오면 그 이후를 접는다. */
export const FOLD_MARKER = ':::spoiler'

/** 리뷰 전체에 대한 스포일러 표시 방식(백엔드 SpoilerMode 와 값이 같아야 한다). */
export const SPOILER_MODES = [
  { value: 'NONE', label: '스포일러 없음' },
  { value: 'BLUR', label: '본문 흐리게 가리기' },
  { value: 'FOLD', label: '본문 접어 두기' },
]

/** 리뷰 공개 범위(백엔드 Visibility 와 값이 같아야 한다). */
export const VISIBILITIES = [
  { value: 'PUBLIC', label: '전체 공개' },
  { value: 'FOLLOWERS', label: '팔로워 공개' },
  { value: 'PRIVATE', label: '나와 상대만' },
]
