/**
 * 리뷰에 붙일 수 있는 감정 태그.
 *
 * 서버에는 code 문자열만 저장하고(review_tags.tag_code), 화면에는 label 을 보여 준다.
 * 태그 목록을 바꿀 때는 이 파일과 백엔드 검증 규칙을 함께 확인한다.
 */
export const REVIEW_TAGS = [
  { code: 'RP', label: 'RP' },
  { code: 'BATTLE', label: '전투' },
  { code: 'MYSTERY', label: '추리' },
  { code: 'HORROR', label: '호러' },
  { code: 'EMOTION', label: '감동' },
  { code: 'COMEDY', label: '코미디' },
  { code: 'BEGINNER', label: '초보 환영' },
  { code: 'CAMPAIGN', label: '장기 캠페인' },
]

/** code → label 조회용 Map. 없는 코드는 code 를 그대로 보여 준다. */
export const REVIEW_TAG_LABELS = new Map(REVIEW_TAGS.map((tag) => [tag.code, tag.label]))
