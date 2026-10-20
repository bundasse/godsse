import { useEffect, useState } from 'react'
import { Link } from 'react-router'

import {
  fetchNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '@/api/notifications.js'
import Avatar from '@/components/Avatar.jsx'

/** 알림 종류별 문구. 앞에 "{닉네임} " 이 붙는다. */
const MESSAGES = {
  REVIEW: '님이 감상을 남겼습니다.',
  LIKE: '님이 감상에 좋아요를 눌렀습니다.',
  REPLY: '님이 감상에 답장을 남겼습니다.',
  FOLLOW: '님이 회원님을 팔로우했습니다.',
}

/** 알림을 누르면 갈 주소. 팔로우는 상대 프로필, 나머지는 감상으로 간다. */
const pathOf = (notification) =>
  notification.type === 'FOLLOW'
    ? `/u/${notification.actor.handle}`
    : `/reviews/${notification.reviewId}`

/**
 * 알림 목록 화면. (로그인 필요)
 *
 * 알림은 만들 수 없고 읽을 수만 있다. 누르면 해당 화면으로 이동하면서 읽음 처리된다.
 */
function NotificationsPage() {
  const [reloadCount, setReloadCount] = useState(0)
  const requestKey = String(reloadCount)
  const [result, setResult] = useState({ key: null, data: null, error: null })

  useEffect(() => {
    let ignore = false

    fetchNotifications()
      .then((data) => {
        if (!ignore) setResult({ key: requestKey, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ key: requestKey, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [requestKey])

  const reload = () => setReloadCount((previous) => previous + 1)

  // 읽음 처리는 부가 동작이라 실패해도 이동을 막지 않는다.
  const handleRead = async (notification) => {
    if (notification.read) {
      return
    }

    try {
      await markNotificationRead(notification.id)
      reload()
    } catch {
      // 무시: 다음에 목록을 열면 다시 읽음 처리할 수 있다.
    }
  }

  const handleReadAll = async () => {
    try {
      await markAllNotificationsRead()
      reload()
    } catch (apiError) {
      window.alert(apiError.message)
    }
  }

  const loading = result.key !== requestKey
  const data = result.key === requestKey ? result.data : null
  const error = result.key === requestKey ? result.error : null

  return (
    <section className="page">
      <h1 className="page__title">알림</h1>

      {data && (
        <div className="notifications__head">
          <p className="text--muted">안 읽은 알림 {data.unreadCount}건</p>
          <button
            className="button button--muted"
            type="button"
            disabled={data.unreadCount === 0}
            onClick={handleReadAll}
          >
            모두 읽음
          </button>
        </div>
      )}

      {loading && <p className="text--muted">불러오는 중...</p>}
      {error && <p className="text--error">{error}</p>}

      {data && data.items.length === 0 && <p className="text--muted">아직 알림이 없습니다.</p>}

      {data && data.items.length > 0 && (
        <ul className="list">
          {data.items.map((notification) => (
            <li
              key={notification.id}
              className={notification.read ? 'notification' : 'notification notification--unread'}
            >
              <Avatar size="tiny" user={notification.actor} />
              <div className="notification__body">
                <Link
                  className="notification__text"
                  to={pathOf(notification)}
                  onClick={() => handleRead(notification)}
                >
                  <strong>{notification.actor.nickname}</strong>{' '}
                  {MESSAGES[notification.type] ?? '님의 새 소식이 있습니다.'}
                </Link>
                <p className="text--muted">
                  {new Date(notification.createdAt).toLocaleDateString('ko-KR')}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export default NotificationsPage
