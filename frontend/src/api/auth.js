import apiClient from '@/api/client.js'

/**
 * 인증 관련 API. 자세한 명세는 doc/05-api.md 참고.
 *
 * 로그인 상태는 서버 세션 + 쿠키로 유지되므로, 프론트에서 토큰을 따로 저장하지 않는다.
 */

/** 회원가입 — 성공하면 프로필을 돌려주고 곧바로 로그인 상태가 된다(201). */
export const signup = ({ handle, email, password, nickname }) =>
  apiClient.post('/api/auth/signup', { handle, email, password, nickname })

/** 로그인 — 성공하면 프로필을 돌려준다. 실패 시 401(ApiError). */
export const login = ({ handle, password }) =>
  apiClient.post('/api/auth/login', { handle, password })

/** 로그아웃 — 서버 세션을 버린다(204, 응답 본문 없음). */
export const logout = () => apiClient.post('/api/auth/logout')

/** 지금 로그인한 사용자. 로그인하지 않았다면 401(ApiError) 이 발생한다. */
export const fetchMe = () => apiClient.get('/api/auth/me')
