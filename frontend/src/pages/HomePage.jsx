import { useEffect, useState } from 'react'
import { Link } from 'react-router'

import { fetchReviews } from '@/api/reviews.js'
import ReviewCard from '@/components/ReviewCard.jsx'
import { useAuth } from '@/context/auth.js'

function HomePage() {
  const { user } = useAuth()
  // 로그인한 사람의 핸들. 이 값이 바뀌면 팔로우 피드를 다시 불러온다.
  const followingKey = user?.handle ?? null

  const [recent, setRecent] = useState({ loaded: false, data: null, error: null })
  const [following, setFollowing] = useState({ key: null, data: null, error: null })

  // 전체 공개 감상(최신순)
  useEffect(() => {
    let ignore = false

    fetchReviews()
      .then((data) => {
        if (!ignore) setRecent({ loaded: true, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setRecent({ loaded: true, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [])

  // 로그인했다면 "내가 팔로우한 사람들의 감상"도 함께 불러온다
  useEffect(() => {
    if (!followingKey) {
      return undefined
    }

    let ignore = false

    fetchReviews({ following: true })
      .then((data) => {
        if (!ignore) setFollowing({ key: followingKey, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setFollowing({ key: followingKey, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [followingKey])

  const followingData = following.key === followingKey ? following.data : null
  const followingLoading = Boolean(followingKey) && following.key !== followingKey

  return (
    <section className="page">
      <h1 className="page__title">godsse</h1>
      <p className="page__description">
        함께 플레이한 사람에게 남긴 감상과, 내가 받은 감상을 모아 보는 공간입니다.
      </p>

      {!user && (
        <div className="card">
          <p className="text--muted">
            <Link to="/signup">회원가입</Link> 하거나 <Link to="/login">로그인</Link> 하면 감상을
            남길 수 있습니다.
          </p>
        </div>
      )}

      {followingKey && (
        <>
          <h2 className="card__title">팔로우한 사람들의 감상</h2>

          {followingLoading && <p className="text--muted">불러오는 중...</p>}

          {followingData &&
            (followingData.total === 0 ? (
              <p className="text--muted">
                아직 팔로우한 사람의 감상이 없습니다. <Link to="/search">유저 검색</Link>에서 사람을
                찾아 팔로우해 보세요.
              </p>
            ) : (
              <ul className="list">
                {followingData.items.map((review) => (
                  <li key={review.id}>
                    <ReviewCard review={review} />
                  </li>
                ))}
              </ul>
            ))}
        </>
      )}

      <h2 className="card__title">최근 공개 감상</h2>

      {!recent.loaded && <p className="text--muted">불러오는 중...</p>}
      {recent.error && <p className="text--error">{recent.error}</p>}

      {recent.data &&
        (recent.data.total === 0 ? (
          <p className="text--muted">
            아직 공개된 감상이 없습니다. <Link to="/reviews/new">첫 감상</Link>을 남겨 보세요!
          </p>
        ) : (
          <ul className="list">
            {recent.data.items.map((review) => (
              <li key={review.id}>
                <ReviewCard review={review} />
              </li>
            ))}
          </ul>
        ))}
    </section>
  )
}

export default HomePage
