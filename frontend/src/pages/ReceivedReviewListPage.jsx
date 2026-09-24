import { useParams } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function ReceivedReviewListPage() {
  const { handle } = useParams()

  return (
    <PagePlaceholder
      title={`@${handle} 님이 받은 감상`}
      description="받은 감상을 카드 목록으로 보여 주는 화면입니다(페이지 단위로 나눠 불러옵니다)."
    />
  )
}

export default ReceivedReviewListPage
