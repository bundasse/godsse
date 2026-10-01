import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'

import { searchUsers } from '@/api/users.js'
import UserCard from '@/components/UserCard.jsx'

function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const keyword = (searchParams.get('q') ?? '').trim()

  const [input, setInput] = useState(keyword)
  // 어떤 검색어의 결과인지 함께 담아 둔다. (ProfilePage 와 같은 이유)
  const [result, setResult] = useState({ keyword: null, data: null, error: null })

  useEffect(() => {
    if (!keyword) {
      return undefined
    }

    let ignore = false

    searchUsers({ q: keyword })
      .then((data) => {
        if (!ignore) setResult({ keyword, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ keyword, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [keyword])

  const handleSubmit = (event) => {
    event.preventDefault()
    const trimmed = input.trim()
    // URL 만 바꾸면 위 effect 가 알아서 다시 불러온다.
    setSearchParams(trimmed ? { q: trimmed } : {})
  }

  const loading = keyword !== '' && result.keyword !== keyword
  const data = result.keyword === keyword ? result.data : null
  const error = result.keyword === keyword ? result.error : null

  return (
    <section className="page">
      <h1 className="page__title">유저 검색</h1>

      <form className="form" onSubmit={handleSubmit}>
        <div className="form__row">
          <label className="form__label" htmlFor="q">
            핸들 또는 닉네임
          </label>
          <input
            id="q"
            className="form__input"
            placeholder="예: godsse"
            value={input}
            onChange={(event) => setInput(event.target.value)}
          />
        </div>
        <button className="button" type="submit">
          검색
        </button>
      </form>

      {!keyword && <p className="text--muted">검색어를 입력하면 결과가 표시됩니다.</p>}
      {loading && <p className="text--muted">검색 중...</p>}
      {error && <p className="text--error">{error}</p>}

      {data && (
        <>
          <p className="text--muted">{data.total}명을 찾았습니다.</p>
          {data.items.length === 0 ? (
            <p className="text--muted">조건에 맞는 사용자가 없습니다.</p>
          ) : (
            <ul className="list">
              {data.items.map((item) => (
                <li key={item.handle}>
                  <UserCard user={item} />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}

export default SearchPage
