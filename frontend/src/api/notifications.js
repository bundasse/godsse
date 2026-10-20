import apiClient from '@/api/client.js'

/**
 * 알림 API. 자세한 명세는 doc/05-api.md 참고.
 *
 * 알림은 서버가 만들어 준다(감상·좋아요·답장·팔로우). 화면은 조회와 읽음 처리만 한다.
 */

/** 내 알림 목록(최신순 최대 30건)과 안 읽은 개수. `{ items, unreadCount }` */
export const fetchNotifications = () => apiClient.get('/api/notifications')

/** 안 읽은 개수만. `{ count }` — 헤더 배지가 사용한다. */
export const fetchUnreadCount = () => apiClient.get('/api/notifications/unread-count')

/** 알림 하나를 읽음으로 표시한다. 성공하면 204, 남의 알림이면 404. */
export const markNotificationRead = (id) => apiClient.post(`/api/notifications/${id}/read`)

/** 내 알림을 모두 읽음으로 표시한다. 성공하면 204. */
export const markAllNotificationsRead = () => apiClient.post('/api/notifications/read-all')
