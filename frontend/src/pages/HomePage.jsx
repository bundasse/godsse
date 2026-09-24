import { Link } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function HomePage() {
  return (
    <PagePlaceholder
      title="godsse"
      description="함께 플레이한 사람에게 남긴 감상과, 내가 받은 감상을 모아 보는 공간입니다."
    >
      <div className="card">
        <h2 className="card__title">화면 준비 중</h2>
        <p className="text--muted">
          다음 단계에서 팔로우한 사람들의 최신 감상이 카드로 표시됩니다. (긴 글은 3줄까지만 보이고,
          누르면 전문 페이지로 이동합니다)
        </p>
        <p className="text--muted">
          먼저 <Link to="/signup">회원가입</Link> 하거나 <Link to="/login">로그인</Link> 해 주세요.
        </p>
      </div>

      <div className="card">
        <h2 className="card__title">만들어질 화면</h2>
        <ul className="text--muted">
          <li>홈 — 최신 감상 피드</li>
          <li>유저 검색 — 핸들·닉네임으로 사람 찾기</li>
          <li>감상 쓰기 — 날짜·룰 필수, 시나리오 선택, 100자 이상</li>
          <li>마이페이지 — 받은 감상 카드 + 세션 일정 달력</li>
        </ul>
      </div>
    </PagePlaceholder>
  )
}

export default HomePage
