/**
 * 프로필 활동 배지.
 *
 * 배지는 서버가 <b>코드</b>로만 계산해 내려주고(`GET /api/users/{handle}` 의 `badges`),
 * 화면에 보일 이름·아이콘은 여기서 정한다. 문구를 고쳐도 서버를 다시 배포할 필요가 없다.
 *
 * 배지 조건을 바꿀 때는 백엔드 `common/ProfileBadges` 를 함께 고친다.
 */
export const PROFILE_BADGES = [
  { code: 'FIRST_REVIEW', emoji: '🖊️', label: '첫 감상', hint: '감상을 처음 남겼어요' },
  { code: 'REVIEW_10', emoji: '📚', label: '감상 10편', hint: '감상을 10편 썼어요' },
  { code: 'RECEIVED_10', emoji: '💌', label: '감상 10편 받음', hint: '감상을 10편 받았어요' },
  {
    code: 'HEARTS_10',
    emoji: '❤️',
    label: '하트 10개',
    hint: '내 감상이 받은 하트가 10개를 넘었어요',
  },
  { code: 'FOLLOWERS_10', emoji: '👥', label: '팔로워 10명', hint: '팔로워가 10명을 넘었어요' },
]

/**
 * code → 배지 정보.
 *
 * 서버가 새 배지를 먼저 내려보내도 화면이 깨지지 않도록, 모르는 코드는 그리지 않는다(`undefined`).
 */
export const PROFILE_BADGE_MAP = new Map(PROFILE_BADGES.map((badge) => [badge.code, badge]))
