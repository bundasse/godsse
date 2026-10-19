import { useState } from 'react'

import { likeReview, unlikeReview } from '@/api/reviews.js'
import { useAuth } from '@/context/auth.js'

/**
 * 감상의 좋아요(하트) 버튼.
 *
 * 좋아요 여부·개수는 부모가 가진 값(서버 응답)을 그대로 쓰고, 누른 뒤 부모가 다시 불러와 갱신한다.
 * (컴포넌트가 따로 상태를 들고 있으면 서버 값과 어긋날 수 있다)
 *
 * @param {object} props
 * @param {number} props.reviewId 대상 감상 id
 * @param {number} props.likeCount 서버가 알려 준 좋아요 수
 * @param {boolean} props.liked 내가 좋아요했는지
 * @param {() => void} [props.onChanged] 성공 후 호출(부모가 감상을 다시 불러옴)
 */
function LikeButton({ reviewId, likeCount, liked, onChanged }) {
  const { user } = useAuth()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  if (!user) {
    return (
      <span className="like">
        <span className="like__heart">{liked ? '♥' : '♡'}</span>
        <span className="like__count">{likeCount}</span>
      </span>
    )
  }

  const handleClick = async () => {
    setBusy(true)
    setError(null)

    try {
      if (liked) {
        await unlikeReview(reviewId)
      } else {
        await likeReview(reviewId)
      }
      if (onChanged) {
        onChanged()
      }
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="like">
      <button
        aria-pressed={liked}
        className={liked ? 'like__button like__button--on' : 'like__button'}
        disabled={busy}
        type="button"
        onClick={handleClick}
      >
        <span className="like__heart">{liked ? '♥' : '♡'}</span>
        <span className="like__count">{likeCount}</span>
      </button>
      {error && <p className="text--error">{error}</p>}
    </div>
  )
}

export default LikeButton
