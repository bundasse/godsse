import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'

import { fetchReview, updateReview } from '@/api/reviews.js'
import ReviewForm from '@/components/ReviewForm.jsx'

/**
 * 감상 수정 화면.
 *
 * 작성 화면과 같은 폼(ReviewForm)을 쓰되, 받는 사람은 바꿀 수 없으므로 표시만 한다.
 */
function ReviewEditPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [result, setResult] = useState({ id: null, review: null, error: null })

  useEffect(() => {
    let ignore = false

    fetchReview(id)
      .then((review) => {
        if (!ignore) setResult({ id, review, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ id, review: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [id])

  if (result.id !== id) {
    return (
      <section className="page">
        <p className="text--muted">감상을 불러오는 중...</p>
      </section>
    )
  }

  if (result.error) {
    return (
      <section className="page">
        <h1 className="page__title">감상 수정</h1>
        <p className="text--error">{result.error}</p>
        <p className="text--muted">
          <Link to={`/reviews/${id}`}>감상으로 돌아가기</Link>
        </p>
      </section>
    )
  }

  const review = result.review

  if (!review.editable) {
    return (
      <section className="page">
        <h1 className="page__title">감상 수정</h1>
        <p className="text--error">이 감상을 수정할 권한이 없습니다.</p>
        <p className="text--muted">
          <Link to={`/reviews/${id}`}>감상으로 돌아가기</Link>
        </p>
      </section>
    )
  }

  /** 받는 사람은 바꿀 수 없으므로 toHandle 은 보내지 않는다. */
  const handleSubmit = async (payload) => {
    await updateReview(id, {
      playedOn: payload.playedOn,
      ruleName: payload.ruleName,
      scenarioName: payload.scenarioName,
      body: payload.body,
      spoilerMode: payload.spoilerMode,
      visibility: payload.visibility,
      tags: payload.tags,
    })
    navigate(`/reviews/${id}`, { replace: true })
  }

  return (
    <section className="page">
      <h1 className="page__title">감상 수정</h1>

      <ReviewForm
        initialValues={{
          toHandle: review.recipient.handle,
          playedOn: review.playedOn,
          ruleName: review.ruleName,
          scenarioName: review.scenarioName ?? '',
          body: review.body,
          spoilerMode: review.spoilerMode,
          visibility: review.visibility,
          tags: review.tags,
        }}
        recipient={review.recipient}
        submitLabel="수정 저장"
        onSubmit={handleSubmit}
      />
    </section>
  )
}

export default ReviewEditPage
