import { useEffect, useState } from 'react'

import { fetchReviews } from '@/api/reviews.js'
import ReviewCard from '@/components/ReviewCard.jsx'

const EMPTY_MESSAGE = {
  received: '아직 받은 감상이 없습니다.',
  written: '아직 쓴 감상이 없습니다.',
}

/**
 * 감상 목록(카드 + 페이지 이동). 받은 감상·쓴 감상 화면이 함께 쓴다.
 *
 * @param {object} props
 * @param {string} props.handle 대상 핸들
 * @param {'received'|'written'} props.kind 받은 감상 / 쓴 감상
 */
function ReviewList({ handle, kind }) {
  const [page, setPage] = useState(0)

  // 어떤 조건의 결과인지 함께 담아 둔다. 조건이 바뀌면 자동으로 "불러오는 중"이 된다.
  const requestKey = `${handle}#${kind}#${page}`
  const [result, setResult] = useState({ key: null, data: null, error: null })

  useEffect(() => {
    let ignore = false

    const query = kind === 'received' ? { recipient: handle, page } : { author: handle, page }

    fetchReviews(query)
      .then((data) => {
        if (!ignore) setResult({ key: requestKey, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ key: requestKey, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [handle, kind, page, requestKey])

  const loading = result.key !== requestKey
  const data = result.key === requestKey ? result.data : null
  const error = result.key === requestKey ? result.error : null

  if (loading) {
    return <p className="text--muted">불러오는 중...</p>
  }

  if (error) {
    return <p className="text--error">{error}</p>
  }

  if (!data) {
    return null
  }

  return (
    <>
      <p className="text--muted">총 {data.total}건</p>

      {data.items.length === 0 ? (
        <p className="text--muted">{EMPTY_MESSAGE[kind]}</p>
      ) : (
        <ul className="list">
          {data.items.map((item) => (
            <li key={item.id}>
              <ReviewCard review={item} />
            </li>
          ))}
        </ul>
      )}

      <div className="form__actions">
        <button
          className="button button--muted"
          disabled={page === 0}
          type="button"
          onClick={() => setPage((previous) => Math.max(previous - 1, 0))}
        >
          이전
        </button>
        <button
          className="button button--muted"
          disabled={!data.hasNext}
          type="button"
          onClick={() => setPage((previous) => previous + 1)}
        >
          다음
        </button>
      </div>
    </>
  )
}

export default ReviewList
