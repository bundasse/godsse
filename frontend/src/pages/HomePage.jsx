import { useEffect, useState } from 'react'
import { Link } from 'react-router'

import { fetchReviews } from '@/api/reviews.js'
import ReviewCard from '@/components/ReviewCard.jsx'
import { useAuth } from '@/context/auth.js'

function HomePage() {
  const { user } = useAuth()
  const [result, setResult] = useState({ loaded: false, data: null, error: null })

  useEffect(() => {
    let ignore = false

    fetchReviews()
      .then((data) => {
        if (!ignore) setResult({ loaded: true, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ loaded: true, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [])

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

      {!result.loaded && <p className="text--muted">불러오는 중...</p>}
      {result.error && <p className="text--error">{result.error}</p>}

      {result.data && (
        <>
          <h2 className="card__title">최근 공개 감상</h2>

          {result.data.total === 0 ? (
            <p className="text--muted">
              아직 공개된 감상이 없습니다. <Link to="/reviews/new">첫 감상</Link>을 남겨 보세요!
            </p>
          ) : (
            <ul className="list">
              {result.data.items.map((review) => (
                <li key={review.id}>
                  <ReviewCard review={review} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}

export default HomePage
