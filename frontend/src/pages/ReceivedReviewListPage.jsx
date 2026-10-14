import { Link, useParams } from 'react-router'

import ReviewList from '@/components/ReviewList.jsx'

function ReceivedReviewListPage() {
  const { handle } = useParams()

  return (
    <section className="page">
      <h1 className="page__title">@{handle} 님이 받은 감상</h1>
      <p className="text--muted">
        <Link to={`/u/${handle}`}>프로필로 돌아가기</Link>
      </p>

      <ReviewList handle={handle} kind="received" />
    </section>
  )
}

export default ReceivedReviewListPage
