import { useEffect, useRef, useState } from 'react'

import { fetchRuleHeatmap, resetRuleColor, saveRuleColor } from '@/api/ruleColors.js'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

/** 색 변경을 모아서 저장하는 시간(ms). 색상 피커는 움직이는 동안 계속 이벤트를 보낸다. */
const SAVE_DELAY = 500

/** Date → 'YYYY-MM-DD' (로컬 시간 기준) */
function toIsoDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 한 해를 "일요일 시작" 주 단위 열로 나눈다(GitHub 잔디 그래프와 같은 모양).
 *
 * 1월 1일 앞과 12월 31일 뒤는 빈 칸(null)으로 채워, 모든 열이 일~토 7칸이 되게 한다.
 *
 * @returns {{ columns: Array<Array<{date: string, month: number, day: number} | null>>,
 *   months: Array<{column: number, label: string}> }}
 */
function buildYearGrid(year) {
  const cells = []
  const first = new Date(year, 0, 1)
  const last = new Date(year, 11, 31)

  for (let index = 0; index < first.getDay(); index += 1) {
    cells.push(null)
  }

  for (const date = new Date(first); date <= last; date.setDate(date.getDate() + 1)) {
    cells.push({ date: toIsoDate(date), month: date.getMonth(), day: date.getDate() })
  }

  while (cells.length % 7 !== 0) {
    cells.push(null)
  }

  const columns = []
  for (let index = 0; index < cells.length; index += 7) {
    columns.push(cells.slice(index, index + 7))
  }

  // 월 라벨은 그 달 1일이 있는 열 위에 붙인다(GitHub 그래프와 같은 방식).
  const months = []
  for (let month = 0; month < 12; month += 1) {
    const column = columns.findIndex((week) =>
      week.some((cell) => cell && cell.month === month && cell.day === 1),
    )

    if (column >= 0) {
      months.push({ column, label: `${month + 1}월` })
    }
  }

  return { columns, months }
}

/** 룰이 여러 개인 날은 대각선 줄무늬로 나눠 칠한다(색이 섞여도 "여러 룰"임이 보인다). */
function cellStyle(colors) {
  if (colors.length === 0) {
    return undefined
  }

  if (colors.length === 1) {
    return { backgroundColor: colors[0] }
  }

  const step = 100 / colors.length
  const stops = colors.map((color, index) => `${color} ${index * step}% ${(index + 1) * step}%`)
  return { backgroundImage: `linear-gradient(135deg, ${stops.join(', ')})` }
}

/**
 * 마이페이지의 연간 룰 히트맵.
 *
 * 서버가 날짜별 룰과 룰별 색을 내려주면, 화면은 7행 × 주 단위 열(일~토)로 칸을 그린다.
 * 범례의 색 견본을 눌러 룰 색을 바꿀 수 있고, 바꾼 색은 잠깐 모았다가 바로 저장한다.
 * (색상 피커는 끄는 동안 계속 값을 보내므로, 매번 저장하면 요청이 쏟아진다)
 *
 * @param {object} props
 * @param {number} props.year 그릴 연도. 세션 통계 카드의 연도 선택을 그대로 쓴다
 */
function RuleHeatmap({ year }) {
  const [reloadCount, setReloadCount] = useState(0)
  const requestKey = `${year}#${reloadCount}`

  const [result, setResult] = useState({ key: null, data: null, error: null })

  // 색상 피커에서 고른 색. 서버 응답보다 먼저 화면에 보여 주려고 따로 들고 있는다.
  const [drafts, setDrafts] = useState({})
  const [saveError, setSaveError] = useState(null)
  const timers = useRef(new Map())

  useEffect(() => {
    let ignore = false

    fetchRuleHeatmap(year)
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

  // 화면을 떠날 때 저장 대기 중인 타이머를 정리한다(떠난 뒤 저장 요청이 나가지 않게).
  useEffect(() => {
    const pending = timers.current

    return () => {
      pending.forEach((timer) => clearTimeout(timer))
      pending.clear()
    }
  }, [])

  const loading = result.key !== requestKey
  const data = result.key === requestKey ? result.data : null
  const error = result.key === requestKey ? result.error : null

  const handleColorChange = (ruleName, color) => {
    setDrafts((previous) => ({ ...previous, [ruleName]: color }))
    setSaveError(null)

    clearTimeout(timers.current.get(ruleName))
    timers.current.set(
      ruleName,
      setTimeout(() => {
        timers.current.delete(ruleName)

        saveRuleColor(ruleName, color)
          .then((saved) => {
            // 서버가 소문자(#rrggbb)로 맞춰 돌려주므로 그 값으로 미리보기를 정리한다.
            setDrafts((previous) => ({ ...previous, [ruleName]: saved.color }))
          })
          .catch((apiError) => {
            // 저장에 실패했으면 서버 값으로 되돌린다(화면에만 바뀐 색이 남지 않게).
            setDrafts((previous) => {
              const next = { ...previous }
              delete next[ruleName]
              return next
            })
            setSaveError(`'${ruleName}' 색을 저장하지 못했습니다. ${apiError.message}`)
          })
      }, SAVE_DELAY),
    )
  }

  const handleReset = (ruleName) => {
    // 대기 중인 저장이 있으면 먼저 취소한다(지운 뒤 옛 색이 저장되면 되살아난다).
    clearTimeout(timers.current.get(ruleName))
    timers.current.delete(ruleName)

    resetRuleColor(ruleName)
      .then(() => {
        setDrafts((previous) => {
          const next = { ...previous }
          delete next[ruleName]
          return next
        })
        setSaveError(null)
        // 기본 팔레트 색은 서버가 계산하므로 다시 받아 온다.
        setReloadCount((previous) => previous + 1)
      })
      .catch((apiError) => {
        setSaveError(`'${ruleName}' 색을 되돌리지 못했습니다. ${apiError.message}`)
      })
  }

  if (loading) {
    return <p className="text--muted">룰 달력을 불러오는 중...</p>
  }

  if (error) {
    return <p className="text--error">{error}</p>
  }

  if (data.totalCount === 0) {
    return <p className="text--muted">{year}년에 기록한 세션이 아직 없습니다.</p>
  }

  const { columns, months } = buildYearGrid(year)
  const byDate = new Map(data.days.map((day) => [day.date, day]))
  const serverColors = new Map(data.rules.map((rule) => [rule.ruleName, rule.color]))
  const today = toIsoDate(new Date())

  const colorOf = (ruleName) => drafts[ruleName] ?? serverColors.get(ruleName) ?? 'var(--color-border)'
  const monthGridStyle = { gridTemplateColumns: `repeat(${columns.length}, var(--heatmap-cell))` }

  return (
    <div className="heatmap">
      <div className="heatmap__title">
        <span>올해의 룰 달력</span>
        <span className="text--muted">
          {data.totalCount}회 · {data.days.length}일
        </span>
      </div>

      <div className="heatmap__scroll">
        <div className="heatmap__chart">
          <div className="heatmap__months" style={monthGridStyle} aria-hidden="true">
            {months.map((month) => (
              <span key={month.label} style={{ gridColumnStart: month.column + 1 }}>
                {month.label}
              </span>
            ))}
          </div>

          <div className="heatmap__weekdays" aria-hidden="true">
            {WEEKDAYS.map((label) => (
              <span key={label}>{label}</span>
            ))}
          </div>

          <div className="heatmap__grid">
            {columns.map((week, weekIndex) => (
              <div className="heatmap__week" key={weekIndex}>
                {week.map((cell, dayIndex) => {
                  if (!cell) {
                    return (
                      <span
                        className="heatmap__cell heatmap__cell--empty"
                        key={`empty-${weekIndex}-${dayIndex}`}
                      />
                    )
                  }

                  const day = byDate.get(cell.date)
                  const colors = (day?.ruleNames ?? []).map(colorOf)
                  const countText = day && day.count > 1 ? ` (${day.count}회)` : ''
                  const label = day
                    ? `${cell.date} · ${day.ruleNames.join(', ')}${countText}`
                    : `${cell.date} · 세션 없음`

                  return (
                    <span
                      className={
                        cell.date === today ? 'heatmap__cell heatmap__cell--today' : 'heatmap__cell'
                      }
                      key={cell.date}
                      style={cellStyle(colors)}
                      title={label}
                    />
                  )
                })}
              </div>
            ))}
          </div>
        </div>
      </div>

      <p className="heatmap__hint">
        색 견본을 눌러 룰 색을 바꿀 수 있습니다. 바꾼 색은 바로 저장됩니다.
      </p>
      {saveError && <p className="text--error">{saveError}</p>}

      <ul className="heatmap__legend" aria-label="룰별 색과 횟수">
        {data.rules.map((rule) => {
          const color = drafts[rule.ruleName] ?? rule.color
          const custom = rule.custom || drafts[rule.ruleName] !== undefined

          return (
            <li className="heatmap__legend-item" key={rule.ruleName}>
              <span
                className="heatmap__swatch"
                style={{ backgroundColor: color }}
                aria-hidden="true"
              />
              <span className="heatmap__rule-name">{rule.ruleName}</span>
              <span className="text--muted">{rule.count}회</span>
              <input
                className="heatmap__color"
                type="color"
                value={color}
                aria-label={`${rule.ruleName} 색 고르기`}
                onChange={(event) => handleColorChange(rule.ruleName, event.target.value)}
              />
              {custom && (
                <button
                  className="button button--muted heatmap__reset"
                  type="button"
                  onClick={() => handleReset(rule.ruleName)}
                >
                  기본색
                </button>
              )}
            </li>
          )
        })}
      </ul>
    </div>
  )
}

export default RuleHeatmap
