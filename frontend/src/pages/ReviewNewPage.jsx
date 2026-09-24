import { useSearchParams } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function ReviewNewPage() {
  const [searchParams] = useSearchParams()
  // 예: /reviews/new?to=godsse — 감상을 받을 사람의 핸들
  const recipientHandle = searchParams.get('to')

  return (
    <PagePlaceholder
      title="감상 쓰기"
      description={
        recipientHandle
          ? `@${recipientHandle} 님에게 남길 감상을 작성하는 화면입니다.`
          : '감상을 받을 사람을 먼저 고르는 화면입니다.'
      }
    >
      <div className="card">
        <h2 className="card__title">입력 항목 (다음 단계에서 연결)</h2>
        <ul className="text--muted">
          <li>플레이 날짜 — 필수</li>
          <li>룰 — 필수 (자유 입력 + 자동완성)</li>
          <li>시나리오명 — 선택</li>
          <li>감상 내용 — 필수, 100자 이상</li>
          <li>스포일러 — 없음 / 흐리게 / 접기 (본문 일부는 ||문장|| 로 가림)</li>
          <li>감정 태그, 공개 범위</li>
        </ul>
      </div>
    </PagePlaceholder>
  )
}

export default ReviewNewPage
