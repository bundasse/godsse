import apiClient from '@/api/client.js'

/**
 * 알림 API. 자세한 명세는 doc/05-api.md 참고.
 *
 * 알림은 서버가 만들어 준다(감상·좋아요·답장·팔로우). 화면은 조회와 읽음 처리만 한다.
 */

// EventSource 는 fetch 래퍼를 쓸 수 없어 주소를 직접 만든다(client.js 와 같은 기준 URL).
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/** 실시간 구독 주소(SSE). 세션 쿠키를 함께 보내야 하므로 EventSource 에 withCredentials 를 준다. */
export const notificationStreamUrl = () => `${BASE_URL}/api/notifications/stream`

/** 내 알림 목록(최신순 최대 30건)과 안 읽은 개수. `{ items, unreadCount }` */
export const fetchNotifications = () => apiClient.get('/api/notifications')

/** 안 읽은 개수만. `{ count }` — 헤더 배지가 사용한다. */
export const fetchUnreadCount = () => apiClient.get('/api/notifications/unread-count')

/** 알림 하나를 읽음으로 표시한다. 성공하면 204, 남의 알림이면 404. */
export const markNotificationRead = (id) => apiClient.post(`/api/notifications/${id}/read`)

/** 내 알림을 모두 읽음으로 표시한다. 성공하면 204. */
export const markAllNotificationsRead = () => apiClient.post('/api/notifications/read-all')
