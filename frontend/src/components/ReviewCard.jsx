import { Link } from 'react-router'

import Avatar from '@/components/Avatar.jsx'
import SpoilerText from '@/components/SpoilerText.jsx'
import { REVIEW_TAG_LABELS } from '@/constants/tags.js'

/**
 * 목록에 쓰는 감상 카드.
 *
 * 본문은 3줄까지만 보이고(서버가 보낸 미리보기 + CSS 말줄임), 누르면 전문 페이지로 이동한다.
 * 카드 안에서는 스포일러를 "눌러서 보기"로 열 수 없게 한다. (카드 전체가 링크라서 클릭이 겹친다)
 *
 * @param {object} props
 * @param {object} props.review ReviewSummary
 */
function ReviewCard({ review }) {
  return (
    <Link className="card card--link review-card" to={`/reviews/${review.id}`}>
      <div className="review-card__head">
        <Avatar size="tiny" user={review.author} />
        <span className="review-card__name">{review.author.nickname}</span>
        <span className="text--muted">→</span>
        <span className="review-card__name">{review.recipient.nickname}</span>
      </div>

      <div className="review-card__meta">
        <span>{review.playedOn}</span>
        <span>{review.ruleName}</span>
        {review.scenarioName && <span>{review.scenarioName}</span>}
        {review.spoilerMode !== 'NONE' && <span className="badge">스포일러 포함</span>}
        <span>
          {review.liked ? '♥' : '♡'} {review.likeCount}
        </span>
      </div>

      <div className="review-card__body">
        <SpoilerText
          interactive={false}
          spoilerMode={review.spoilerMode}
          text={review.bodyPreview}
        />
      </div>

      {review.tags.length > 0 && (
        <div className="tags">
          {review.tags.map((tag) => (
            <span className="chip" key={tag}>
              {REVIEW_TAG_LABELS.get(tag) ?? tag}
            </span>
          ))}
        </div>
      )}
    </Link>
  )
}

export default ReviewCard
