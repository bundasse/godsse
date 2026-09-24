import { Link } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function NotFoundPage() {
  return (
    <PagePlaceholder
      title="페이지를 찾을 수 없습니다"
      description="주소가 바뀌었거나 존재하지 않는 화면입니다."
    >
      <p className="text--muted">
        <Link to="/">홈으로 돌아가기</Link>
      </p>
    </PagePlaceholder>
  )
}

export default NotFoundPage
