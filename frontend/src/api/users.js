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

/** 팔로우(로그인 필요). 성공하면 204(응답 본문 없음). */
export const followUser = (handle) =>
  apiClient.post(`/api/users/${encodeURIComponent(handle)}/follow`)

/** 언팔로우(로그인 필요). 성공하면 204. */
export const unfollowUser = (handle) =>
  apiClient.delete(`/api/users/${encodeURIComponent(handle)}/follow`)

/** 팔로워 목록(이 사람을 팔로우하는 사람들). */
export const fetchFollowers = (handle, { page = 0, size = 20 } = {}) =>
  apiClient.get(`/api/users/${encodeURIComponent(handle)}/followers?page=${page}&size=${size}`)

/** 팔로잉 목록(이 사람이 팔로우하는 사람들). */
export const fetchFollowing = (handle, { page = 0, size = 20 } = {}) =>
  apiClient.get(`/api/users/${encodeURIComponent(handle)}/following?page=${page}&size=${size}`)
