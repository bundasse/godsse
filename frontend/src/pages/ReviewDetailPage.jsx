import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'

import { deleteReview, fetchReview } from '@/api/reviews.js'
import Avatar from '@/components/Avatar.jsx'
import LikeButton from '@/components/LikeButton.jsx'
import ReviewReplies from '@/components/ReviewReplies.jsx'
import SpoilerText from '@/components/SpoilerText.jsx'
import { VISIBILITIES } from '@/constants/spoilerMarkers.js'
import { REVIEW_TAG_LABELS } from '@/constants/tags.js'

const visibilityLabel = (value) => VISIBILITIES.find((item) => item.value === value)?.label ?? value

function ReviewDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  // 좋아요·답장 뒤에 감상을 다시 불러오기 위한 카운터
  const [reloadCount, setReloadCount] = useState(0)
  const requestKey = `${id}#${reloadCount}`

  // 어떤 요청의 결과인지 함께 담아 둔다. 조건이 바뀌면 자동으로 "아직 안 불러옴" 상태가 된다.
  const [result, setResult] = useState({ key: null, review: null, error: null })

  useEffect(() => {
    let ignore = false

    fetchReview(id)
      .then((review) => {
        if (!ignore) setResult({ key: requestKey, review, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ key: requestKey, review: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [id, requestKey])

  const reload = () => {
    setReloadCount((previous) => previous + 1)
  }

  const handleDelete = async () => {
    if (!window.confirm('이 감상을 삭제할까요? 되돌릴 수 없습니다.')) {
      return
    }

    try {
      await deleteReview(id)
      navigate('/me', { replace: true })
    } catch (apiError) {
      window.alert(apiError.message)
    }
  }

  if (result.key !== requestKey) {
    return (
      <section className="page">
        <p className="text--muted">감상을 불러오는 중...</p>
      </section>
    )
  }

  if (result.error) {
    return (
      <section className="page">
        <h1 className="page__title">감상 전문</h1>
        <p className="text--error">{result.error}</p>
        <p className="text--muted">
          <Link to="/">홈으로 가기</Link>
        </p>
      </section>
    )
  }

  const review = result.review

  return (
    <section className="page">
      <article className="card">
        <div className="review-detail__head">
          <Avatar size="medium" user={review.author} />
          <div>
            <p className="review-detail__names">
              <Link to={`/u/${review.author.handle}`}>{review.author.nickname}</Link>
              <span className="text--muted"> → </span>
              <Link to={`/u/${review.recipient.handle}`}>{review.recipient.nickname}</Link>
            </p>
            <p className="text--muted">
              {review.playedOn} · {review.ruleName}
              {review.scenarioName ? ` · ${review.scenarioName}` : ''}
            </p>
          </div>
        </div>

        <div className="review-card__meta">
          <span className="badge">{visibilityLabel(review.visibility)}</span>
          {review.spoilerMode !== 'NONE' && <span className="badge">스포일러 포함</span>}
        </div>

        <SpoilerText spoilerMode={review.spoilerMode} text={review.body} />

        {review.tags.length > 0 && (
          <div className="tags">
            {review.tags.map((tag) => (
              <span className="chip" key={tag}>
                {REVIEW_TAG_LABELS.get(tag) ?? tag}
              </span>
            ))}
          </div>
        )}

        <LikeButton
          likeCount={review.likeCount}
          liked={review.liked}
          reviewId={review.id}
          onChanged={reload}
        />

        {review.editable && (
          <div className="form__actions">
            <Link className="button" to={`/reviews/${review.id}/edit`}>
              수정
            </Link>
            <button className="button button--muted" type="button" onClick={handleDelete}>
              삭제
            </button>
          </div>
        )}
      </article>

      <section className="card">
        <ReviewReplies onChanged={reload} reviewId={review.id} />
      </section>

      <p className="text--muted">
        <Link to={`/u/${review.recipient.handle}/received`}>이 사람이 받은 감상 전체 보기</Link>
      </p>
    </section>
  )
}

export default ReviewDetailPage
