/**
 * 프로필 이미지를 보여 주는 작은 컴포넌트.
 *
 * 이미지가 없으면 이름의 첫 글자를 보여 준다.
 * (이미지가 깨진 경우까지 처리하려면 onError 로 상태를 바꾸는 방법이 있지만,
 *  지금은 업로드한 파일만 쓰므로 없을 때의 대체 표시만 다룬다)
 *
 * @param {object} props
 * @param {{ nickname?: string, handle?: string, profileImageUrl?: string }} props.user
 * @param {'tiny'|'small'|'medium'|'large'} [props.size] 크기 (기본 medium)
 */
function Avatar({ user, size = 'medium' }) {
  const label = user?.nickname || user?.handle || '?'
  const className = `avatar avatar--${size}`

  if (user?.profileImageUrl) {
    return (
      <img alt={`${label} 의 프로필 이미지`} className={className} src={user.profileImageUrl} />
    )
  }

  return (
    <span aria-hidden="true" className={`${className} avatar--fallback`}>
      {label.slice(0, 1)}
    </span>
  )
}

export default Avatar
