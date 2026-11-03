import apiClient from '@/api/client.js'

/**
 * 룰 히트맵(연간 그래프) + 룰 색 API.
 *
 * 일정과 색은 모두 "내 것"만 다루므로 로그인이 필요하다.
 * 룰 이름은 자유 입력이라 쿼리·본문에 넣을 때 `encodeURIComponent` 로 감싼다.
 */

/** 한 해의 날짜별 룰 히트맵. `year` 를 생략하면 올해. */
export const fetchRuleHeatmap = (year) =>
  apiClient.get(year ? `/api/sessions/heatmap?year=${year}` : '/api/sessions/heatmap')

/**
 * 룰 색 저장(없으면 새로 만들고, 있으면 바꾼다).
 * 서버가 소문자로 맞춰 저장하므로 돌려받는 `color` 는 `#rrggbb` 형식이다.
 */
export const saveRuleColor = (ruleName, color) =>
  apiClient.put('/api/users/me/rule-colors', { ruleName, color })

/** 룰 색을 지워 기본 팔레트 색으로 되돌린다. 성공하면 204(본문 없음). */
export const resetRuleColor = (ruleName) =>
  apiClient.delete(`/api/users/me/rule-colors?ruleName=${encodeURIComponent(ruleName)}`)
