import { useState } from 'react'
import { Link } from 'react-router'

import { addSessionParticipant, removeSessionParticipant } from '@/api/sessions.js'

/**
 * 세션 일정에 "함께한 사람"을 기록하는 영역.
 *
 * 핸들로 가입한 사용자를 찾아 연결하고, 그 사람의 프로필과 **감상 작성 링크**로 이어 준다.
 * (세션을 마친 뒤 "누구에게 감상을 쓸지"를 기억에서 꺼내지 않아도 되게 하는 것이 목적)
 *
 * @param {object} props
 * @param {object} props.plan 일정(`participants` 포함)
 * @param {Function} props.onChanged 참가자가 바뀌었을 때 목록을 다시 불러오는 함수
 */
function SessionParticipants({ plan, onChanged }) {
  const [handle, setHandle] = useState('')
  const [error, setError] = useState(null)
  const [pending, setPending] = useState(false)

  const participants = plan.participants ?? []

  const run = async (action) => {
    setPending(true)
    setError(null)

    try {
      await action()
      onChanged()
    } catch (apiError) {
      // 400/404/409 모두 서버가 한국어 메시지를 준다. (예: 이미 참가자로 기록된 사람입니다)
      setError(apiError.message)
    } finally {
      setPending(false)
    }
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    const trimmed = handle.trim()
    if (!trimmed || pending) {
      return
    }

    run(async () => {
      await addSessionParticipant(plan.id, trimmed)
      setHandle('')
    })
  }

  const handleRemove = (person) => {
    if (pending) {
      return
    }

    run(() => removeSessionParticipant(plan.id, person.handle))
  }

  return (
    <div className="plan__people">
      <p className="text--muted">
        함께한 사람 {participants.length}명
        {participants.length === 0 && ' · 세션을 마친 뒤 기록해 두면 감상을 쓰기 쉬워요'}
      </p>

      {participants.length > 0 && (
        <ul className="profile__badges">
          {participants.map((person) => (
            <li className="badge plan__person" key={person.handle}>
              <Link to={`/u/${person.handle}`}>{person.nickname}</Link>
              <Link to={`/reviews/new?to=${person.handle}`} title="이 사람에게 감상 남기기">
                감상
              </Link>
              <button
                type="button"
                disabled={pending}
                onClick={() => handleRemove(person)}
                aria-label={`${person.nickname} 참가자에서 빼기`}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}

      <form className="form__inline" onSubmit={handleSubmit}>
        <input
          className="form__input"
          value={handle}
          onChange={(event) => setHandle(event.target.value)}
          placeholder="@ 없이 핸들"
          aria-label="함께한 사람 핸들"
        />
        <button className="button button--muted" type="submit" disabled={pending}>
          추가
        </button>
      </form>

      {error && <p className="form__error">{error}</p>}
    </div>
  )
}

export default SessionParticipants
