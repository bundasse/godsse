import { useParams } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function WrittenReviewListPage() {
  const { handle } = useParams()

  return (
    <PagePlaceholder
      title={`@${handle} 님이 쓴 감상`}
      description="내가(또는 다른 사람이) 남긴 감상 목록을 보여 주는 화면입니다."
    />
  )
}

export default WrittenReviewListPage
