import { Link } from 'react-router'

import Avatar from '@/components/Avatar.jsx'

/**
 * 사용자 요약 카드. (검색 결과 등)
 *
 * @param {object} props
 * @param {{ handle: string, nickname: string, profileImageUrl: string|null }} props.user
 */
function UserCard({ user }) {
  return (
    <Link className="card card--link user-card" to={`/u/${user.handle}`}>
      <Avatar size="small" user={user} />
      <span className="user-card__name">{user.nickname}</span>
      <span className="user-card__handle">@{user.handle}</span>
    </Link>
  )
}

export default UserCard
