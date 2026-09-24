import { Outlet } from 'react-router'

import Header from '@/components/Header.jsx'
import NavBar from '@/components/NavBar.jsx'

import './AppLayout.css'

/**
 * 모든 화면의 공통 뼈대.
 *
 * <p>
 * 상단 헤더(로고 + PC 메뉴), 본문, 모바일 하단 탭으로 구성된다.
 * <Outlet /> 자리에 현재 URL 에 맞는 페이지가 들어간다(Vue Router 의 <router-view /> 와 같은 역할).
 */
function AppLayout() {
  return (
    <div className="layout">
      <Header />
      <main className="layout__main">
        <Outlet />
      </main>
      <NavBar />
    </div>
  )
}

export default AppLayout
