import { useState } from 'react'

import { followUser, unfollowUser } from '@/api/users.js'
import { useAuth } from '@/context/auth.js'

/**
 * 프로필 화면의 팔로우/언팔로우 버튼.
 *
 * <p>
 * 팔로우 여부는 이 컴포넌트가 눌린 즉시 바꾸고(빠른 반응), 서버에 다시 조회해 확인한다.
 * 부모는 결과적으로 `isFollowing` 이 달라지면 이 컴포넌트를 새로 마운트한다(key 사용).
 *
 * @param {object} props
 * @param {string} props.handle 대상 핸들
 * @param {boolean} props.isFollowing 현재 팔로우 중인지 (서버가 알려 준 값)
 * @param {() => void} [props.onChanged] 팔로우 상태가 바뀐 뒤 호출(부모의 프로필 새로고침)
 */
function FollowButton({ handle, isFollowing, onChanged }) {
  const { user } = useAuth()
  const [following, setFollowing] = useState(isFollowing)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  if (!user) {
    return <p className="text--muted">팔로우하려면 로그인이 필요합니다.</p>
  }

  const handleClick = async () => {
    setBusy(true)
    setError(null)

    try {
      if (following) {
        await unfollowUser(handle)
      } else {
        await followUser(handle)
      }
      setFollowing(!following)
      if (onChanged) {
        onChanged()
      }
    } catch (apiError) {
      setError(apiError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="follow">
      <button
        className={following ? 'button button--muted' : 'button'}
        disabled={busy}
        type="button"
        onClick={handleClick}
      >
        {following ? '언팔로우' : '팔로우'}
      </button>
      {error && <p className="text--error">{error}</p>}
    </div>
  )
}

export default FollowButton
