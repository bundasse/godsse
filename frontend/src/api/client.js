const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/**
 * API 오류를 담는 Error 확장판.
 *
 * 서버는 실패할 때 아래 형태로 응답한다.
 * { "message": "...", "errors": [ { "field": "handle", "reason": "..." } ] }
 *
 * status(HTTP 상태 코드)와 errors(필드별 오류)를 함께 들고 있어 화면에서 활용할 수 있다.
 * 예: 409(중복 핸들) → errors 에서 handle 오류를 찾아 입력칸 아래에 표시.
 */
export class ApiError extends Error {
  constructor(message, status, errors = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }

  /** 특정 입력칸(field)의 오류 메시지를 찾는다. 없으면 null. */
  fieldError(field) {
    return this.errors.find((error) => error.field === field)?.reason ?? null
  }
}

/**
 * 공통 fetch 래퍼. 개발 중에는 vite.config.js 의 프록시(/api -> 8080)를 사용한다.
 * 서버가 4xx/5xx 를 반환하면 ApiError 를 던지므로 호출부에서 try/catch 로 처리한다.
 */
async function request(path, options = {}) {
  const { headers, ...rest } = options

  const response = await fetch(`${BASE_URL}${path}`, {
    // 로그인 상태는 세션 쿠키(JSESSIONID)로 유지되므로 쿠키를 주고받아야 한다.
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    ...rest,
  })

  // 204 No Content 는 본문이 없다.
  if (response.status === 204) {
    return null
  }

  const text = await response.text()

  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      // JSON 이 아닌 응답(HTML 오류 페이지 등)은 본문 없음으로 취급한다.
      data = null
    }
  }

  if (!response.ok) {
    const message = data?.message ?? `요청에 실패했습니다. (HTTP ${response.status})`
    throw new ApiError(message, response.status, data?.errors ?? [])
  }

  return data
}

export const apiClient = {
  get: (path) => request(path, { method: 'GET' }),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
}

export default apiClient
