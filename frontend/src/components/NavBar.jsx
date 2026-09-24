import { NavLink } from 'react-router'

const linkClass = ({ isActive }) =>
  isActive ? 'navbar__link navbar__link--active' : 'navbar__link'

/** 모바일 하단 탭 항목. (PC 에서는 CSS 로 숨긴다) */
const ITEMS = [
  { to: '/', label: '홈' },
  { to: '/search', label: '검색' },
  { to: '/reviews/new', label: '감상 쓰기' },
  { to: '/me', label: '마이페이지' },
]

function NavBar() {
  return (
    <nav className="navbar">
      {ITEMS.map((item) => (
        <NavLink key={item.to} className={linkClass} end={item.to === '/'} to={item.to}>
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}

export default NavBar
