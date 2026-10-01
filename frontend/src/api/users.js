import apiClient from '@/api/client.js'

/**
 * 사용자(프로필) API. 자세한 명세는 doc/05-api.md 참고.
 */

/** 프로필 조회(로그인 불필요). 없는 핸들이면 404(ApiError). */
export const fetchUserProfile = (handle) =>
  apiClient.get(`/api/users/${encodeURIComponent(handle)}`)

/**
 * 프로필 편집(닉네임·자기소개). 로그인 필요.
 * 핸들은 주소에 쓰이므로 이 API 로 바꿀 수 없다.
 */
export const updateMyProfile = ({ nickname, bio }) =>
  apiClient.patch('/api/users/me', { nickname, bio })

/**
 * 프로필 이미지 업로드(multipart/form-data, 필드 이름 file).
 * 파일 전송은 JSON 이 아니라서 전용 메서드(postForm)를 쓴다.
 */
export const uploadMyAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.postForm('/api/users/me/avatar', formData)
}

/** 유저 검색(로그인 불필요). 검색어가 비면 서버가 빈 목록을 돌려준다. */
export const searchUsers = ({ q, page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (q) {
    params.set('q', q)
  }
  return apiClient.get(`/api/users?${params.toString()}`)
}
