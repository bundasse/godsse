import apiClient from '@/api/client.js'

/**
 * 감상(리뷰) API. 자세한 명세는 doc/05-api.md 참고.
 *
 * 스포일러 마커(||...||, :::spoiler)는 서버가 해석하지 않고 그대로 저장한다.
 */

/** 감상 작성. 성공하면 201 과 감상 전문을 돌려준다. */
export const createReview = (body) => apiClient.post('/api/reviews', body)

/** 감상 전문. 공개 범위 밖이면 403(ApiError). */
export const fetchReview = (id) => apiClient.get(`/api/reviews/${id}`)

/** 감상 목록.
 *
 * - 인자를 주지 않으면 홈 피드(전체 공개, 최신순)
 * - `{ following: true }` → 내가 팔로우한 사람들의 감상 (로그인 필요)
 * - `{ author }` → 그 사람이 쓴 감상
 * - `{ recipient }` → 그 사람이 받은 감상
 */
export const fetchReviews = ({
  author,
  recipient,
  following = false,
  page = 0,
  size = 20,
} = {}) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (author) {
    params.set('author', author)
  }
  if (recipient) {
    params.set('recipient', recipient)
  }
  if (following) {
    params.set('following', 'true')
  }
  return apiClient.get(`/api/reviews?${params.toString()}`)
}

/** 감상 수정(작성자만 가능, 아니면 403). */
export const updateReview = (id, body) => apiClient.patch(`/api/reviews/${id}`, body)

/** 감상 삭제(작성자만 가능, 아니면 403). 성공하면 204. */
export const deleteReview = (id) => apiClient.delete(`/api/reviews/${id}`)

/** 좋아요(로그인 필요). 이미 눌렀으면 409, 볼 수 없는 감상이면 403. */
export const likeReview = (id) => apiClient.post(`/api/reviews/${id}/like`)

/** 좋아요 취소. 누르지 않았으면 404. */
export const unlikeReview = (id) => apiClient.delete(`/api/reviews/${id}/like`)

/** 답장 목록(오래된 순). */
export const fetchReplies = (id) => apiClient.get(`/api/reviews/${id}/replies`)

/** 답장 작성(로그인 필요, 500자 이하). */
export const createReply = (id, body) => apiClient.post(`/api/reviews/${id}/replies`, { body })

/** 답장 삭제(답장 작성자만). 성공하면 204. */
export const deleteReply = (id, replyId) =>
  apiClient.delete(`/api/reviews/${id}/replies/${replyId}`)
