import { useParams } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function ReviewDetailPage() {
  const { id } = useParams()

  return (
    <PagePlaceholder
      title="감상 전문"
      description={`리뷰 #${id} 의 전체 내용이 표시될 자리입니다. 카드에서 3줄만 보이던 글이 여기서는 전부 펼쳐집니다.`}
    />
  )
}

export default ReviewDetailPage
