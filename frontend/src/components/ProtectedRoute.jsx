import { Navigate, useLocation } from 'react-router'

import { useAuth } from '@/context/auth.js'

/**
 * 로그인한 사람만 볼 수 있는 화면을 감싼다.
 *
 * <p>
 * 로그인하지 않았다면 로그인 화면으로 보내고, 로그인에 성공하면 원래 가려던 주소로 돌아온다.
 * (로그인 화면에서 location.state.from 을 읽어 처리한다)
 *
 * @param {object} props
 * @param {React.ReactNode} props.children 보호할 화면
 */
function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()
  const location = useLocation()

  // 앱을 새로 열었을 때는 서버에 로그인 여부를 물어보는 동안 잠깐 기다린다.
  if (loading) {
    return <p className="text--muted">로그인 정보를 확인하는 중...</p>
  }

  if (!user) {
    return <Navigate replace state={{ from: location.pathname }} to="/login" />
  }

  return children
}

export default ProtectedRoute
