import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'

import { useAuth } from '@/context/auth.js'

function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [handle, setHandle] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      await login({ handle, password })
      // 보호된 화면에 들어가려다 밀려온 경우, 원래 가려던 주소로 돌아간다.
      navigate(location.state?.from ?? '/me', { replace: true })
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="page">
      <h1 className="page__title">로그인</h1>
      <p className="page__description">가입할 때 정한 핸들과 비밀번호를 입력해 주세요.</p>

      <form className="form" onSubmit={handleSubmit}>
        <div className="form__row">
          <label className="form__label" htmlFor="handle">
            핸들
          </label>
          <input
            id="handle"
            className="form__input"
            autoComplete="username"
            value={handle}
            onChange={(event) => setHandle(event.target.value)}
          />
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="password">
            비밀번호
          </label>
          <input
            id="password"
            className="form__input"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>

        {error && <p className="text--error">{error}</p>}

        <button className="button" type="submit" disabled={submitting}>
          {submitting ? '로그인 중...' : '로그인'}
        </button>
      </form>

      <p className="text--muted">
        아직 계정이 없나요? <Link to="/signup">회원가입</Link>
      </p>
    </section>
  )
}

export default LoginPage
