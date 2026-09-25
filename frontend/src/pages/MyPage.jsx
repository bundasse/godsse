import { Link } from 'react-router'

import { useAuth } from '@/context/auth.js'

function MyPage() {
  const { user, logout } = useAuth()

  const handleLogout = () => {
    logout()
  }

  return (
    <section className="page">
      <h1 className="page__title">마이페이지</h1>
      <p className="page__description">
        {user.nickname} (@{user.handle}) 님, 반갑습니다.
      </p>

      <div className="card">
        <h2 className="card__title">내 정보</h2>
        <dl className="definition">
          <dt>핸들</dt>
          <dd>@{user.handle}</dd>
          <dt>닉네임</dt>
          <dd>{user.nickname}</dd>
          <dt>자기소개</dt>
          <dd>{user.bio ?? '(아직 없음)'}</dd>
          <dt>가입일</dt>
          <dd>{new Date(user.createdAt).toLocaleDateString('ko-KR')}</dd>
        </dl>
        <p className="text--muted">
          <Link to="/me/edit">프로필 편집</Link> 은 다음 단계(1-2)에서 연결합니다.
        </p>
      </div>

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

      <button className="button button--muted" type="button" onClick={handleLogout}>
        로그아웃
      </button>
    </section>
  )
}

export default MyPage
