import { useSearchParams } from 'react-router'

import PagePlaceholder from '@/components/PagePlaceholder.jsx'

function SearchPage() {
  const [searchParams] = useSearchParams()
  const keyword = searchParams.get('q') ?? ''

  return (
    <PagePlaceholder
      title="유저 검색"
      description={
        keyword
          ? `"${keyword}" 검색 결과가 표시될 자리입니다.`
          : '핸들 또는 닉네임으로 사람을 찾습니다.'
      }
    />
  )
}

export default SearchPage
