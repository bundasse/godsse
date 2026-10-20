import { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router'

import { fetchUnreadCount } from '@/api/notifications.js'
import { useAuth } from '@/context/auth.js'

/** 현재 경로와 같으면 강조 표시를 붙인다. (Header 와 같은 규칙) */
const linkClass = ({ isActive }) =>
  isActive ? 'header__link header__link--active' : 'header__link'

/**
 * 헤더의 알림 링크 + 안 읽은 개수 배지.
 *
 * 주기적으로 물어보지 않고, <b>화면을 옮길 때마다</b> 한 번 세어 본다.
 * (알림 화면에서 읽음 처리하고 돌아오면 배지가 사라진다)
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

  return (
    <NavLink className={linkClass} to="/notifications">
      알림
      {count > 0 && <span className="header__badge">{count > 99 ? '99+' : count}</span>}
    </NavLink>
  )
}

export default NotificationBell
