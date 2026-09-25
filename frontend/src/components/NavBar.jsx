import { NavLink } from 'react-router'

import { useAuth } from '@/context/auth.js'

const linkClass = ({ isActive }) =>
  isActive ? 'navbar__link navbar__link--active' : 'navbar__link'

function NavBar() {
  const { user } = useAuth()

  /** 모바일 하단 탭 항목. (PC 에서는 CSS 로 숨긴다) */
  const items = [
    { to: '/', label: '홈' },
    { to: '/search', label: '검색' },
    { to: '/reviews/new', label: '감상 쓰기' },
    { to: '/me', label: user ? '마이페이지' : '로그인' },
  ]

  return (
    <nav className="navbar">
      {items.map((item) => (
        <NavLink key={item.to} className={linkClass} end={item.to === '/'} to={item.to}>
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}

export default NavBar
