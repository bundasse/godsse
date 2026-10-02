import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'

import { fetchFollowers, fetchFollowing } from '@/api/users.js'
import UserCard from '@/components/UserCard.jsx'

/**
 * 팔로워 / 팔로잉 목록 화면. (한 화면에서 탭으로 전환한다)
 *
 * 주소: {@code /u/{핸들}/follows?tab=followers|following}
 */
function FollowListPage() {
  const { handle } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = searchParams.get('tab') === 'following' ? 'following' : 'followers'

  const [page, setPage] = useState(0)
  // 어떤 조건(핸들+탭+페이지)의 결과인지 함께 담아 둔다. 조건이 바뀌면 자동으로 "불러오는 중"이 된다.
  const requestKey = `${handle}#${tab}#${page}`
  const [result, setResult] = useState({ key: null, data: null, error: null })

  useEffect(() => {
    let ignore = false
    const fetcher = tab === 'followers' ? fetchFollowers : fetchFollowing

    fetcher(handle, { page })
      .then((data) => {
        if (!ignore) setResult({ key: requestKey, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ key: requestKey, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [handle, tab, page, requestKey])

  // 탭을 바꾸면 페이지를 처음으로 되돌린다. (이벤트 핸들러라 setState 를 바로 호출해도 된다)
  const changeTab = (nextTab) => {
    setPage(0)
    setSearchParams({ tab: nextTab })
  }

  const loading = result.key !== requestKey
  const data = result.key === requestKey ? result.data : null
  const error = result.key === requestKey ? result.error : null

  return (
    <section className="page">
      <h1 className="page__title">@{handle}</h1>

      <div className="tabs">
        <button
          className={tab === 'followers' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
          type="button"
          onClick={() => changeTab('followers')}
        >
          팔로워
        </button>
        <button
          className={tab === 'following' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
          type="button"
          onClick={() => changeTab('following')}
        >
          팔로잉
        </button>
      </div>

      {loading && <p className="text--muted">불러오는 중...</p>}
      {error && <p className="text--error">{error}</p>}

      {data && (
        <>
          <p className="text--muted">총 {data.total}명</p>

          {data.items.length === 0 ? (
            <p className="text--muted">
              {tab === 'followers'
                ? '아직 이 사람을 팔로우한 사람이 없습니다.'
                : '아직 이 사람이 팔로우한 사람이 없습니다.'}
            </p>
          ) : (
            <ul className="list">
              {data.items.map((item) => (
                <li key={item.handle}>
                  <UserCard user={item} />
                </li>
              ))}
            </ul>
          )}

          <div className="form__actions">
            <button
              className="button button--muted"
              disabled={page === 0}
              type="button"
              onClick={() => setPage((previous) => Math.max(previous - 1, 0))}
            >
              이전
            </button>
            <button
              className="button button--muted"
              disabled={!data.hasNext}
              type="button"
              onClick={() => setPage((previous) => previous + 1)}
            >
              다음
            </button>
          </div>
        </>
      )}

      <p className="text--muted">
        <Link to={`/u/${handle}`}>프로필로 돌아가기</Link>
      </p>
    </section>
  )
}

export default FollowListPage
