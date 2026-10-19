import { useEffect, useState } from 'react'
import { Link } from 'react-router'

import { createReply, deleteReply, fetchReplies } from '@/api/reviews.js'
import Avatar from '@/components/Avatar.jsx'
import { useAuth } from '@/context/auth.js'

/**
 * 감상에 달린 답장 목록 + 작성 폼.
 *
 * 답장은 한 단계만 둔다(답장에 답장 없음).
 *
 * @param {object} props
 * @param {number} props.reviewId 대상 감상 id
 * @param {() => void} [props.onChanged] 답장이 추가·삭제된 뒤 호출(부모가 감상을 다시 불러옴)
 */
function ReviewReplies({ reviewId, onChanged }) {
  const { user } = useAuth()

  const [reloadCount, setReloadCount] = useState(0)
  const requestKey = `${reviewId}#${reloadCount}`
  const [result, setResult] = useState({ key: null, items: null, error: null })

  const [body, setBody] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let ignore = false

    fetchReplies(reviewId)
      .then((items) => {
        if (!ignore) setResult({ key: requestKey, items, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ key: requestKey, items: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [reviewId, requestKey])

  const reload = () => {
    setReloadCount((previous) => previous + 1)
    if (onChanged) {
      onChanged()
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      await createReply(reviewId, body)
      setBody('')
      reload()
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (replyId) => {
    if (!window.confirm('이 답장을 삭제할까요?')) {
      return
    }

    try {
      await deleteReply(reviewId, replyId)
      reload()
    } catch (apiError) {
      window.alert(apiError.message)
    }
  }

  const loading = result.key !== requestKey
  const items = result.key === requestKey ? result.items : null
  const listError = result.key === requestKey ? result.error : null

  return (
    <div className="replies">
      <h2 className="card__title">답장{items ? ` (${items.length})` : ''}</h2>

      {loading && <p className="text--muted">답장을 불러오는 중...</p>}
      {listError && <p className="text--error">{listError}</p>}

      {items && items.length === 0 && <p className="text--muted">아직 답장이 없습니다.</p>}

      {items && items.length > 0 && (
        <ul className="list">
          {items.map((reply) => (
            <li className="reply" key={reply.id}>
              <Avatar size="tiny" user={reply.author} />
              <div className="reply__body">
                <p className="reply__head">
                  <Link to={`/u/${reply.author.handle}`}>{reply.author.nickname}</Link>
                  <span className="text--muted">
                    {' · '}
                    {new Date(reply.createdAt).toLocaleDateString('ko-KR')}
                  </span>
                </p>
                <p className="reply__text">{reply.body}</p>
                {reply.editable && (
                  <button
                    className="link-button"
                    type="button"
                    onClick={() => handleDelete(reply.id)}
                  >
                    삭제
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {user ? (
        <form className="form" onSubmit={handleSubmit}>
          <div className="form__row">
            <label className="form__label" htmlFor="replyBody">
              답장 남기기 (500자 이하)
            </label>
            <textarea
              id="replyBody"
              className="form__input form__textarea"
              value={body}
              onChange={(event) => setBody(event.target.value)}
            />
          </div>

          {error && <p className="text--error">{error}</p>}

          <div className="form__actions">
            <button className="button" type="submit" disabled={submitting || !body.trim()}>
              {submitting ? '올리는 중...' : '답장 남기기'}
            </button>
          </div>
        </form>
      ) : (
        <p className="text--muted">
          <Link to="/login">로그인</Link> 하면 답장을 남길 수 있습니다.
        </p>
      )}
    </div>
  )
}

export default ReviewReplies
