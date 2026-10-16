import { useNavigate, useSearchParams } from 'react-router'

import { createReview } from '@/api/reviews.js'
import ReviewForm from '@/components/ReviewForm.jsx'

/**
 * 감상 작성 화면.
 *
 * 실제 입력 폼은 {@link ReviewForm} 에 있고(수정 화면과 공용), 이 화면은
 * 시작 값과 저장 동작만 정한다.
 */
function ReviewNewPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const initialValues = {
    toHandle: searchParams.get('to') ?? '',
    playedOn: new Date().toISOString().slice(0, 10),
    ruleName: '',
    scenarioName: '',
    body: '',
    spoilerMode: 'NONE',
    visibility: 'PUBLIC',
    tags: [],
  }

  const handleSubmit = async (payload) => {
    const created = await createReview(payload)
    navigate(`/reviews/${created.id}`, { replace: true })
  }

  return (
    <section className="page">
      <h1 className="page__title">감상 쓰기</h1>
      <p className="page__description">
        함께 플레이한 사람에게, 그 세션(1회 플레이)에 대한 감상을 남깁니다.
      </p>

      <ReviewForm initialValues={initialValues} submitLabel="감상 남기기" onSubmit={handleSubmit} />
    </section>
  )
}

export default ReviewNewPage
