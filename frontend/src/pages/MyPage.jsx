import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function MyPage() {
  return (
    <PagePlaceholder
      title="마이페이지"
      description="받은 감상과 내 세션 일정을 한 곳에서 관리합니다."
    >
      <div className="card">
        <h2 className="card__title">받은 감상 (탭 1)</h2>
        <p className="text--muted">
          카드 형태로 보여 주고, 긴 글은 3줄까지만 표시한 뒤 클릭하면 전문 페이지로 이동합니다.
        </p>
      </div>

      <div className="card">
        <h2 className="card__title">세션 일정 달력 (탭 2)</h2>
        <p className="text--muted">
          앞으로 플레이할 세션을 날짜별로 기록합니다. 플레이를 마친 일정은 감상 쓰기로 이어집니다.
        </p>
      </div>
    </PagePlaceholder>
  )
}

export default MyPage
