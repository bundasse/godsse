import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router'

import { fetchUnreadCount, notificationStreamUrl } from '@/api/notifications.js'
import { useAuth } from '@/context/auth.js'

/** 현재 경로와 같으면 강조 표시를 붙인다. (Header 와 같은 규칙) */
const linkClass = ({ isActive }) =>
  isActive ? 'header__link header__link--active' : 'header__link'

/**
 * 헤더의 알림 링크 + 안 읽은 개수 배지.
 *
 * 개수를 얻는 길이 두 가지다.
 * 1) **실시간(SSE)** — 서버가 개수가 바뀔 때마다 밀어 준다(감상·좋아요·답장·팔로우·읽음).
 * 2) **화면 이동 때 한 번 조회** — SSE 가 막힌 환경(프록시·오래된 브라우저)에서도 배지가 동작하도록 남긴 안전망.
 */
function NotificationBell() {
  const { user } = useAuth()
  const location = useLocation()
  const [count, setCount] = useState(0)

  useEffect(() => {
    if (!user) {
      return undefined
    }

    let ignore = false

    fetchUnreadCount()
      .then((data) => {
        if (!ignore) setCount(data.count)
      })
      .catch(() => {
        // 배지는 곁다리 정보라 실패하면 이전 값을 그대로 둔다.
      })

    return () => {
      ignore = true
    }
  }, [user, location.pathname])

  // 2) 실시간 — 서버가 밀어 주는 개수로 바로 갱신한다(SSE).
  useEffect(() => {
    if (!user) {
      return undefined
    }

    const source = new EventSource(notificationStreamUrl(), { withCredentials: true })

    const handle = (event) => {
      try {
        setCount(JSON.parse(event.data).count)
      } catch {
        // 형식이 다르면 무시한다(다음 이벤트에서 다시 맞춰진다).
      }
    }

    source.addEventListener('connected', handle)
    source.addEventListener('changed', handle)

    // 끊기면 EventSource 가 스스로 다시 연결한다. 화면을 떠날 때만 닫는다.
    return () => source.close()
  }, [user])

  return (
    <NavLink className={linkClass} to="/notifications">
      알림
      {count > 0 && <span className="header__badge">{count > 99 ? '99+' : count}</span>}
    </NavLink>
  )
}

export default NotificationBell
