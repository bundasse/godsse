import { useEffect, useState } from 'react'

import { createSession, deleteSession, fetchSessions, updateSession } from '@/api/sessions.js'
import SessionForm from '@/components/SessionForm.jsx'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

const STATUS_LABELS = { PLANNED: '예정', DONE: '완료', CANCELED: '취소' }

/** Date → 'YYYY-MM-DD' (로컬 시간 기준) */
function toIsoDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** 그 달의 1일 ~ 말일 (다음 달 0일 = 이번 달 말일) */
function monthRange(year, month) {
  return {
    from: toIsoDate(new Date(year, month, 1)),
    to: toIsoDate(new Date(year, month + 1, 0)),
  }
}

/** 일요일 시작 달력 칸. 빈 칸은 null */
function buildWeeks(year, month) {
  const cells = []
  const firstWeekday = new Date(year, month, 1).getDay()
  const lastDay = new Date(year, month + 1, 0).getDate()

  for (let index = 0; index < firstWeekday; index += 1) {
    cells.push(null)
  }
  for (let day = 1; day <= lastDay; day += 1) {
    cells.push(day)
  }
  while (cells.length % 7 !== 0) {
    cells.push(null)
  }

  const weeks = []
  for (let index = 0; index < cells.length; index += 7) {
    weeks.push(cells.slice(index, index + 7))
  }
  return weeks
}

/**
 * 마이페이지의 세션 일정 달력.
 *
 * 한 달치 일정을 불러와 달력에 점으로 표시하고, 날짜를 고르면 그 날 일정과 작성 폼을 보여 준다.
 */
function MyCalendar() {
  const today = new Date()

  const [view, setView] = useState({ year: today.getFullYear(), month: today.getMonth() })
  const [selectedDate, setSelectedDate] = useState(toIsoDate(today))
  // null | { mode: 'create' } | { mode: 'edit', plan }
  const [formState, setFormState] = useState(null)
  const [reloadCount, setReloadCount] = useState(0)

  const range = monthRange(view.year, view.month)
  const requestKey = `${range.from}#${range.to}#${reloadCount}`
  const [result, setResult] = useState({ key: null, items: null, error: null })

  useEffect(() => {
    let ignore = false

    fetchSessions({ from: range.from, to: range.to })
      .then((items) => {
        if (!ignore) setResult({ key: requestKey, items, error: null })
      })
      .catch((apiError) => {
        if (!ignore) setResult({ key: requestKey, items: null, error: apiError.message })
      })

    return () => {
      ignore = true
    }
  }, [range.from, range.to, requestKey])

  const loading = result.key !== requestKey
  const items = result.key === requestKey ? result.items : null
  const error = result.key === requestKey ? result.error : null

  const reload = () => {
    setReloadCount((previous) => previous + 1)
    setFormState(null)
  }

  /** 이 날짜(YYYY-MM-DD)의 일정들 */
  const plansOf = (date) => (items ?? []).filter((plan) => plan.plannedDate === date)

  const moveMonth = (delta) => {
    setView((previous) => {
      const next = new Date(previous.year, previous.month + delta, 1)
      return { year: next.getFullYear(), month: next.getMonth() }
    })
  }

  const goToday = () => {
    const current = new Date()
    setView({ year: current.getFullYear(), month: current.getMonth() })
    setSelectedDate(toIsoDate(current))
    setFormState(null)
  }

  const handleCreate = async (payload) => {
    await createSession(payload)
    reload()
  }

  const handleUpdate = async (payload) => {
    await updateSession(formState.plan.id, payload)
    reload()
  }

  const handleDelete = async (plan) => {
    if (!window.confirm(`"${plan.title}" 일정을 삭제할까요?`)) {
      return
    }

    try {
      await deleteSession(plan.id)
      reload()
    } catch (apiError) {
      window.alert(apiError.message)
    }
  }

  const weeks = buildWeeks(view.year, view.month)
  const dayPlans = plansOf(selectedDate)

  return (
    <div className="calendar">
      <div className="calendar__header">
        <button
          aria-label="이전 달"
          className="button button--muted"
          type="button"
          onClick={() => moveMonth(-1)}
        >
          ‹
        </button>
        <strong className="calendar__title">
          {view.year}년 {view.month + 1}월
        </strong>
        <button
          aria-label="다음 달"
          className="button button--muted"
          type="button"
          onClick={() => moveMonth(1)}
        >
          ›
        </button>
        <button className="button button--muted" type="button" onClick={goToday}>
          오늘
        </button>
      </div>

      <table className="calendar__grid">
        <thead>
          <tr>
            {WEEKDAYS.map((label) => (
              <th key={label} scope="col">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {weeks.map((week, weekIndex) => (
            <tr key={weekIndex}>
              {week.map((day, dayIndex) => {
                if (day === null) {
                  return <td className="calendar__cell" key={`blank-${dayIndex}`} />
                }

                const date = toIsoDate(new Date(view.year, view.month, day))
                const dayItems = plansOf(date)

                return (
                  <td className="calendar__cell" key={date}>
                    <button
                      className={[
                        'calendar__day',
                        date === selectedDate ? 'calendar__day--selected' : '',
                        date === toIsoDate(today) ? 'calendar__day--today' : '',
                      ]
                        .join(' ')
                        .trim()}
                      type="button"
                      onClick={() => {
                        setSelectedDate(date)
                        setFormState(null)
                      }}
                    >
                      <span className="calendar__day-number">{day}</span>
                      {dayItems.slice(0, 2).map((plan) => (
                        <span className="calendar__dot" key={plan.id} title={plan.title} />
                      ))}
                      {dayItems.length > 2 && (
                        <span className="calendar__more">+{dayItems.length - 2}</span>
                      )}
                    </button>
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>

      {loading && <p className="text--muted">일정을 불러오는 중...</p>}
      {error && <p className="text--error">{error}</p>}

      <div className="calendar__panel">
        <div className="calendar__panel-head">
          <h3 className="card__title">{selectedDate} 일정</h3>
          <button className="button" type="button" onClick={() => setFormState({ mode: 'create' })}>
            일정 추가
          </button>
        </div>

        {dayPlans.length === 0 ? (
          <p className="text--muted">이 날은 아직 일정이 없습니다.</p>
        ) : (
          <ul className="list">
            {dayPlans.map((plan) => (
              <li className="card" key={plan.id}>
                <div className="calendar__plan-head">
                  <strong>{plan.title}</strong>
                  <span
                    className={
                      plan.status === 'PLANNED'
                        ? 'badge'
                        : `badge badge--${plan.status.toLowerCase()}`
                    }
                  >
                    {STATUS_LABELS[plan.status]}
                  </span>
                  {plan.gm && <span className="badge">GM</span>}
                </div>

                <p className="text--muted">
                  {plan.ruleName}
                  {plan.scenarioName ? ` · ${plan.scenarioName}` : ''}
                </p>
                {plan.memo && <p className="text--muted">{plan.memo}</p>}

                <div className="form__actions">
                  <button
                    className="button button--muted"
                    type="button"
                    onClick={() => setFormState({ mode: 'edit', plan })}
                  >
                    수정
                  </button>
                  <button
                    className="button button--muted"
                    type="button"
                    onClick={() => handleDelete(plan)}
                  >
                    삭제
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {formState && (
          <SessionForm
            initialValues={
              formState.mode === 'edit'
                ? {
                    title: formState.plan.title,
                    plannedDate: formState.plan.plannedDate,
                    ruleName: formState.plan.ruleName,
                    scenarioName: formState.plan.scenarioName ?? '',
                    gm: formState.plan.gm,
                    memo: formState.plan.memo ?? '',
                    status: formState.plan.status,
                  }
                : {
                    title: '',
                    plannedDate: selectedDate,
                    ruleName: '',
                    scenarioName: '',
                    gm: false,
                    memo: '',
                    status: 'PLANNED',
                  }
            }
            key={formState.mode === 'edit' ? `edit-${formState.plan.id}` : `create-${selectedDate}`}
            submitLabel={formState.mode === 'edit' ? '수정 저장' : '일정 추가'}
            onCancel={() => setFormState(null)}
            onSubmit={formState.mode === 'edit' ? handleUpdate : handleCreate}
          />
        )}
      </div>
    </div>
  )
}

export default MyCalendar
