import { Link, NavLink } from 'react-router'

/** 현재 경로와 같으면 강조 표시를 붙인다(Vue Router 의 router-link-active 와 비슷). */
const linkClass = ({ isActive }) =>
  isActive ? 'header__link header__link--active' : 'header__link'

function Header() {
  return (
    <header className="header">
      <Link className="header__brand" to="/">
        godsse
      </Link>

      <nav className="header__nav">
        <NavLink className={linkClass} to="/search">
          유저 검색
        </NavLink>
        <NavLink className={linkClass} to="/reviews/new">
          감상 쓰기
        </NavLink>
        <NavLink className={linkClass} to="/me">
          마이페이지
        </NavLink>
        <NavLink className={linkClass} to="/login">
          로그인
        </NavLink>
      </nav>
    </header>
  )
}

export default Header
