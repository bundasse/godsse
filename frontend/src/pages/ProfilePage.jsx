import { useParams } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function ProfilePage() {
  const { handle } = useParams()

  return (
    <PagePlaceholder
      title={`@${handle}`}
      description="프로필(닉네임·자기소개·프로필 이미지)과 팔로우 버튼, 받은 감상 카드가 표시될 자리입니다."
    />
  )
}

export default ProfilePage
