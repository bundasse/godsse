import { useEffect, useState } from 'react'

import { fetchSessionStats } from '@/api/sessions.js'
import RuleHeatmap from '@/components/RuleHeatmap.jsx'

/**
 * 마이페이지의 연간 세션 통계 카드.
 *
 * 서버가 한 해 일정을 한 번에 읽어 월별·상태·룰별로 세어 준다(연간 수십 건이라 충분하다).
 * 막대 높이는 그 해 최다 월을 100% 로 두고 상대 비교한다.
 */
function SessionStats() {
  const currentYear = new Date().getFullYear()
  const [year, setYear] = useState(currentYear)

  const requestKey = String(year)
  const [result, setResult] = useState({ key: null, data: null, error: null })

  useEffect(() => {
    let ignore = false

    fetchSessionStats(year)
      .then((data) => {
        if (!ignore) setResult({ key: requestKey, data, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ key: requestKey, data: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [year, requestKey])

  const loading = result.key !== requestKey
  const data = result.key === requestKey ? result.data : null
  const error = result.key === requestKey ? result.error : null

  // 0 으로 나누지 않도록 최소 1. (모두 0회일 때는 막대를 그리지 않는다)
  const peak = data ? Math.max(1, ...data.monthlyCounts) : 1

  return (
    <div className="stats">
      <div className="stats__head">
        <h2 className="card__title">세션 기록</h2>
        <div className="stats__year">
          <button
            className="button button--muted"
            type="button"
            onClick={() => setYear((previous) => previous - 1)}
            aria-label="이전 해"
          >
            ‹
          </button>
          <strong>{year}년</strong>
          <button
            className="button button--muted"
            type="button"
            onClick={() => setYear((previous) => previous + 1)}
            aria-label="다음 해"
          >
            ›
          </button>
        </div>
      </div>

      {loading && <p className="text--muted">불러오는 중...</p>}
      {error && <p className="text--error">{error}</p>}

      {data && data.totalCount === 0 && (
        <p className="text--muted">{year}년에 기록한 세션이 아직 없습니다.</p>
      )}

      {data && data.totalCount > 0 && (
        <>
          <p className="text--muted">
            모두 {data.totalCount}회 · 완료 {data.doneCount} · 예정 {data.plannedCount} · 취소{' '}
            {data.canceledCount} · GM {data.gmCount}회
          </p>

          <ul className="stats__months" aria-label="월별 세션 횟수">
            {data.monthlyCounts.map((count, index) => (
              <li className="stats__month" key={index} title={`${index + 1}월 ${count}회`}>
                <span
                  className="stats__bar"
                  style={{
                    height: count === 0 ? '2px' : `${Math.max(8, Math.round((count / peak) * 100))}%`,
                  }}
                />
                <span className="stats__month-label">{index + 1}</span>
              </li>
            ))}
          </ul>

          {/* 룰별 횟수는 히트맵 범례가 함께 보여 준다(색까지 바꿀 수 있다). */}
          <RuleHeatmap year={year} />
        </>
      )}
    </div>
  )
}

export default SessionStats
