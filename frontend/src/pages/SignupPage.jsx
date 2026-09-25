import { useState } from 'react'
import { Link, useNavigate } from 'react-router'

import { useAuth } from '@/context/auth.js'

const INITIAL_FORM = { handle: '', email: '', password: '', nickname: '' }

/** 입력 항목 정의 — 라벨과 설명을 한곳에서 관리한다. */
const FIELDS = [
  {
    name: 'handle',
    label: '핸들 (고유 아이디)',
    hint: '영문·숫자·밑줄 3~20자. 주소(/u/핸들)에 쓰이며 나중에 바꿀 수 없습니다.',
    type: 'text',
    autoComplete: 'username',
  },
  {
    name: 'email',
    label: '이메일',
    hint: '중복 가입을 막고, 나중에 비밀번호 찾기에 사용합니다.',
    type: 'email',
    autoComplete: 'email',
  },
  {
    name: 'password',
    label: '비밀번호',
    hint: '8자 이상. 서버에는 암호화된 값만 저장됩니다.',
    type: 'password',
    autoComplete: 'new-password',
  },
  {
    name: 'nickname',
    label: '닉네임',
    hint: '화면에 보이는 이름(20자 이하). 언제든 바꿀 수 있습니다.',
    type: 'text',
    autoComplete: 'nickname',
  },
]

function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState(INITIAL_FORM)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const handleChange = (name) => (event) => {
    const { value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      await signup(form)
      navigate('/me', { replace: true })
    } catch (apiError) {
      setError(apiError.message)

      // 서버가 필드별 오류(errors)를 주면 해당 입력칸 아래에 표시한다.
      const list = apiError.errors ?? []
      if (list.length > 0) {
        setFieldErrors(Object.fromEntries(list.map((item) => [item.field, item.reason])))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="page">
      <h1 className="page__title">회원가입</h1>
      <p className="page__description">
        핸들은 아이디처럼 쓰이는 고유 이름이고, 닉네임은 화면에 보이는 이름입니다.
      </p>

      <form className="form" onSubmit={handleSubmit}>
        {FIELDS.map((field) => (
          <div className="form__row" key={field.name}>
            <label className="form__label" htmlFor={field.name}>
              {field.label}
            </label>
            <input
              id={field.name}
              className="form__input"
              type={field.type}
              autoComplete={field.autoComplete}
              value={form[field.name]}
              onChange={handleChange(field.name)}
            />
            <p className="form__hint">{field.hint}</p>
            {fieldErrors[field.name] && <p className="form__error">{fieldErrors[field.name]}</p>}
          </div>
        ))}

        {error && <p className="text--error">{error}</p>}

        <button className="button" type="submit" disabled={submitting}>
          {submitting ? '가입 중...' : '가입하기'}
        </button>
      </form>

      <p className="text--muted">
        이미 계정이 있나요? <Link to="/login">로그인</Link>
      </p>
    </section>
  )
}

export default SignupPage
