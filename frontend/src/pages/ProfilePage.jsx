import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'

import { fetchUserProfile } from '@/api/users.js'
import Avatar from '@/components/Avatar.jsx'
import { useAuth } from '@/context/auth.js'

function ProfilePage() {
  const { handle } = useParams()
  const { user: me } = useAuth()

  // 어떤 핸들의 결과인지 함께 담아 둔다. handle 이 바뀌면 자동으로 "아직 안 불러옴" 상태가 된다.
  // (effect 본문에서 곧바로 setState 하면 eslint react-hooks/set-state-in-effect 규칙에 걸린다)
  const [result, setResult] = useState({ handle: null, profile: null, error: null })

  useEffect(() => {
    let ignore = false

    fetchUserProfile(handle)
      .then((profile) => {
        if (!ignore) setResult({ handle, profile, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ handle, profile: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [handle])

  const loading = result.handle !== handle

  if (loading) {
    return (
      <section className="page">
        <p className="text--muted">프로필을 불러오는 중...</p>
      </section>
    )
  }

  if (result.error) {
    return (
      <section className="page">
        <h1 className="page__title">@{handle}</h1>
        <p className="text--error">{result.error}</p>
        <p className="text--muted">
          <Link to="/search">유저 검색</Link> 으로 이동
        </p>
      </section>
    )
  }

  const profile = result.profile
  const isMe = me?.handle === profile.handle

  return (
    <section className="page">
      <div className="card profile">
        <div className="profile__head">
          <Avatar size="large" user={profile} />
          <div>
            <p className="profile__name">{profile.nickname}</p>
            <p className="profile__handle">@{profile.handle}</p>
          </div>
        </div>

        <p className="profile__bio text--muted">{profile.bio ?? '아직 자기소개가 없습니다.'}</p>

        <div className="profile__stats">
          <span>받은 감상 {profile.receivedReviewCount}</span>
          <span>쓴 감상 {profile.writtenReviewCount}</span>
          <span>{new Date(profile.createdAt).toLocaleDateString('ko-KR')} 가입</span>
        </div>

        {isMe ? (
          <p className="text--muted">
            <Link to="/me/edit">프로필 편집</Link>
          </p>
        ) : (
          <p className="text--muted">팔로우 기능은 다음 단계(1-3)에서 연결합니다.</p>
        )}
      </div>

      <div className="card">
        <h2 className="card__title">받은 감상</h2>
        <p className="text--muted">
          <Link to={`/u/${profile.handle}/received`}>받은 감상 전체 보기</Link> — 감상 기능은 1-4
          에서 연결합니다.
        </p>
      </div>
    </section>
  )
}

export default ProfilePage
