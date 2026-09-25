import { useCallback, useEffect, useMemo, useState } from 'react'

import { fetchMe, login as loginApi, logout as logoutApi, signup as signupApi } from '@/api/auth.js'
import { AuthContext } from '@/context/auth.js'

/**
 * 로그인 상태를 앱 전체에 공급하는 컴포넌트.
 *
 * <p>
 * Vue 의 Pinia 에서 auth 스토어 하나를 두고 쓰는 것과 같은 역할이다.
 * 새로고침해도 서버 세션 쿠키가 남아 있으면 자동으로 로그인 상태가 복원된다.
 *
 * @param {object} props
 * @param {React.ReactNode} props.children 하위 컴포넌트
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const showUser = useCallback((profile) => {
    setUser(profile)
    setLoading(false)
  }, [])

  // 앱이 처음 뜰 때 "이미 로그인되어 있는지" 서버에 물어본다.
  // setState 는 effect 본문이 아니라 .then/.catch 콜백에서 호출한다.
  // (eslint react-hooks/set-state-in-effect 규칙)
  useEffect(() => {
    let ignore = false

    fetchMe()
      .then((profile) => {
        if (!ignore) showUser(profile)
      })
      .catch(() => {
        // 401 은 "로그인하지 않음"이라는 정상 상태이므로 오류로 취급하지 않는다.
        if (!ignore) showUser(null)
      })

    return () => {
      ignore = true
    }
  }, [showUser])

  const signup = useCallback(async (form) => {
    const profile = await signupApi(form)
    setUser(profile)
    return profile
  }, [])

  const login = useCallback(async (form) => {
    const profile = await loginApi(form)
    setUser(profile)
    return profile
  }, [])

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } finally {
      // 서버 요청이 실패하더라도 화면은 로그아웃 상태로 만든다.
      setUser(null)
    }
  }, [])

  const value = useMemo(
    () => ({ user, loading, signup, login, logout }),
    [user, loading, signup, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default AuthProvider
