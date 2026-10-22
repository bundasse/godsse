import apiClient from '@/api/client.js'

/**
 * 세션 일정(달력) API. 모두 로그인이 필요하다.
 *
 * 달력은 한 달치를 한 번에 그리므로 기간(`from`~`to`, 'YYYY-MM-DD')으로 조회한다.
 */

/** 기간 내 내 일정 목록. */
export const fetchSessions = ({ from, to }) => apiClient.get(`/api/sessions?from=${from}&to=${to}`)

/** 일정 작성. 성공하면 201. */
export const createSession = (body) => apiClient.post('/api/sessions', body)

/** 일정 수정(상태 변경 포함). 본인 일정만 가능. */
export const updateSession = (id, body) => apiClient.patch(`/api/sessions/${id}`, body)

/** 일정 삭제. 성공하면 204. */
export const deleteSession = (id) => apiClient.delete(`/api/sessions/${id}`)

/**
 * 일정에 "함께한 사람" 추가(핸들). 성공하면 204.
 * 가입한 사용자만 기록할 수 있다(404 = 없는 핸들, 409 = 이미 있음, 400 = 나 자신).
 */
export const addSessionParticipant = (id, handle) =>
  apiClient.post(`/api/sessions/${id}/participants`, { handle })

/** 일정에서 참가자 제거. 성공하면 204. */
export const removeSessionParticipant = (id, handle) =>
  apiClient.delete(`/api/sessions/${id}/participants/${encodeURIComponent(handle)}`)
