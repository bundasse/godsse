import { createContext, useContext } from 'react'

/**
 * 로그인 사용자 정보를 담는 컨텍스트.
 *
 * 컴포넌트와 훅을 파일로 나눈 이유: ESLint 의 react-refresh 규칙이
 * "컴포넌트 파일에서는 컴포넌트만 export" 하도록 요구하기 때문이다.
 * (Vue 의 Pinia 스토어 정의와 사용을 분리해 두는 것과 비슷하다)
 */
export const AuthContext = createContext(null)

/** 로그인 상태를 쓰고 싶은 컴포넌트에서 호출한다. AuthProvider 안에서만 동작한다. */
export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth 는 AuthProvider 안에서만 사용할 수 있습니다.')
  }

  return context
}
