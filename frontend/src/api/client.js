const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * 공통 fetch 래퍼. 개발 중에는 vite.config.js 의 프록시(/api -> 8080)를 사용한다.
 * 서버가 4xx/5xx 를 반환하면 Error 를 던지므로 호출부에서 try/catch 로 처리한다.
 */
async function request(path, options = {}) {
  const { headers, ...rest } = options

  const response = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    ...rest,
  })

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    const message = data?.message ?? `요청에 실패했습니다. (HTTP ${response.status})`
    throw new Error(message)
  }

  return data
}

export const apiClient = {
  get: (path) => request(path, { method: 'GET' }),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
}

export default apiClient
