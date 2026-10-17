import { Link, useSearchParams } from 'react-router'

import MyCalendar from '@/components/MyCalendar.jsx'
import ReviewList from '@/components/ReviewList.jsx'
import { useAuth } from '@/context/auth.js'

function MyPage() {
  const { user, logout } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()

  // ?tab=calendar 이면 달력, 그 외에는 받은 감상
  const tab = searchParams.get('tab') === 'calendar' ? 'calendar' : 'reviews'

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

      <div className="tabs">
        <button
          className={tab === 'reviews' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
          type="button"
          onClick={() => setSearchParams({})}
        >
          받은 감상
        </button>
        <button
          className={tab === 'calendar' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
          type="button"
          onClick={() => setSearchParams({ tab: 'calendar' })}
        >
          세션 일정
        </button>
      </div>

      {tab === 'reviews' ? (
        <div className="card">
          <h2 className="card__title">받은 감상</h2>
          <ReviewList handle={user.handle} kind="received" />
          <p className="text--muted">
            <Link to={`/u/${user.handle}/received`}>받은 감상 전체 보기</Link>
          </p>
        </div>
      ) : (
        <div className="card">
          <h2 className="card__title">세션 일정 달력</h2>
          <p className="text--muted">날짜를 눌러 그 날의 일정을 확인하거나 추가하세요.</p>
          <MyCalendar />
        </div>
      )}
    </section>
  )
}

export default MyPage
