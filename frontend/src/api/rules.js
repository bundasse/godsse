import apiClient from '@/api/client.js'

/**
 * 룰(규칙 체계) 목록 API. 로그인 없이 볼 수 있는 참조 데이터다.
 *
 * 폼을 열 때마다 요청하지 않도록 한 번 받은 목록을 기억한다.
 * (룰 목록은 거의 바뀌지 않고, 응답이 작아도 매번 왕복할 이유가 없다)
 */
let cached = null

export const fetchRules = () => {
  if (!cached) {
    cached = apiClient.get('/api/rules').catch(() => {
      // 실패하면 기억하지 않고 다음에 다시 시도한다. (폼은 기본 목록으로 동작)
      cached = null
      return []
    })
  }

  return cached
}
