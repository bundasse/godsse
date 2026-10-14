import { Link } from 'react-router'

import ReviewList from '@/components/ReviewList.jsx'
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

        <div className="form__actions">
          <Link className="button button--muted" to="/me/edit">
            프로필 편집
          </Link>
          <Link className="button button--muted" to={`/u/${user.handle}`}>
            공개 프로필
          </Link>
          <button className="button button--muted" type="button" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      </div>

      <div className="card">
        <h2 className="card__title">받은 감상</h2>
        <ReviewList handle={user.handle} kind="received" />
        <p className="text--muted">
          <Link to={`/u/${user.handle}/received`}>받은 감상 전체 보기</Link>
        </p>
      </div>

      <div className="card">
        <h2 className="card__title">세션 일정 달력</h2>
        <p className="text--muted">
          앞으로 플레이할 세션을 날짜별로 기록합니다. (다음 단계에서 연결합니다)
        </p>
      </div>
    </section>
  )
}

export default MyPage
