import { useState } from 'react'
import { Link } from 'react-router'

import {
  addSessionParticipant,
  removeSessionParticipant,
  requestSessionReviews,
} from '@/api/sessions.js'

/**
 * 감상 요청 결과를 한 줄로 안내한다.
 *
 * "아무도 못 받았다"를 한 문장으로 뭉뚱그리면 이유를 알 수 없어서, 서버가 나눠 준 세 숫자를 그대로 쓴다.
 * (이미 감상을 씀 / 아직 읽지 않은 요청이 있음 / 이번에 보냄)
 */
function requestSummary({ requestedCount, alreadyReviewedCount, alreadyRequestedCount }) {
  if (requestedCount > 0) {
    const skipped =
      alreadyRequestedCount > 0 ? ` (아직 읽지 않아 ${alreadyRequestedCount}명은 건너뜀)` : ''
    return `${requestedCount}명에게 감상 요청 알림을 보냈습니다.${skipped}`
  }

  if (alreadyRequestedCount > 0) {
    return '이미 요청을 보냈고 아직 읽지 않았습니다. 상대가 읽은 뒤 다시 보낼 수 있어요.'
  }

  return alreadyReviewedCount > 0
    ? '모두 이미 감상을 남겨서 보낼 요청이 없습니다.'
    : '요청을 보낼 수 있는 사람이 없습니다.'
}

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
  // 감상 요청 결과 (null = 아직 보내지 않음)
  const [requested, setRequested] = useState(null)

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

  /** 함께한 사람들에게 감상 요청 알림을 보낸다. (이미 감상을 쓴 사람은 서버가 뺀다) */
  const handleRequest = () => {
    if (pending) {
      return
    }

    setRequested(null)
    run(async () => {
      const result = await requestSessionReviews(plan.id)
      setRequested(result)
    })
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

      {participants.length > 0 && (
        <div className="form__actions">
          <button className="button button--muted" type="button" disabled={pending} onClick={handleRequest}>
            감상 요청 보내기
          </button>
        </div>
      )}

      {requested !== null && (
        <p className={requested.requestedCount > 0 ? 'text--success' : 'text--muted'}>
          {requestSummary(requested)}
        </p>
      )}

      {error && <p className="form__error">{error}</p>}
    </div>
  )
}

export default SessionParticipants
