import { useState } from 'react'

import { DEFAULT_RULES } from '@/constants/rules.js'

const STATUS_OPTIONS = [
  { value: 'PLANNED', label: '예정' },
  { value: 'DONE', label: '완료' },
  { value: 'CANCELED', label: '취소' },
]

/**
 * 세션 일정 작성·수정 공용 폼.
 *
 * @param {object} props
 * @param {object} props.initialValues title·plannedDate·ruleName·scenarioName·gm·memo·status
 * @param {string} props.submitLabel 저장 버튼 문구
 * @param {(payload: object) => Promise<void>} props.onSubmit 저장 요청(실패하면 예외를 던져 폼이 표시)
 * @param {() => void} [props.onCancel] 취소 버튼(없으면 숨김)
 */
function SessionForm({ initialValues, submitLabel, onSubmit, onCancel }) {
  const [form, setForm] = useState(initialValues)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const updateField = (name) => (event) => {
    const { type, checked, value } = event.target
    setForm((previous) => ({ ...previous, [name]: type === 'checkbox' ? checked : value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      await onSubmit(form)
    } catch (apiError) {
      setError(apiError.message)

      const list = apiError.errors ?? []
      if (list.length > 0) {
        setFieldErrors(Object.fromEntries(list.map((item) => [item.field, item.reason])))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <div className="form__row">
        <label className="form__label" htmlFor="planTitle">
          일정 제목 (필수)
        </label>
        <input
          id="planTitle"
          className="form__input"
          placeholder="예: 정기 캠페인 3화"
          value={form.title}
          onChange={updateField('title')}
        />
        {fieldErrors.title && <p className="form__error">{fieldErrors.title}</p>}
      </div>

      <div className="form__row">
        <label className="form__label" htmlFor="planDate">
          날짜 (필수)
        </label>
        <input
          id="planDate"
          className="form__input"
          type="date"
          value={form.plannedDate}
          onChange={updateField('plannedDate')}
        />
        {fieldErrors.plannedDate && <p className="form__error">{fieldErrors.plannedDate}</p>}
      </div>

      <div className="form__row">
        <label className="form__label" htmlFor="planRule">
          룰 (필수)
        </label>
        <input
          id="planRule"
          className="form__input"
          list="plan-rule-options"
          placeholder="예: 크툴루의 부름 7판"
          value={form.ruleName}
          onChange={updateField('ruleName')}
        />
        <datalist id="plan-rule-options">
          {DEFAULT_RULES.map((rule) => (
            <option key={rule} value={rule} />
          ))}
        </datalist>
        {fieldErrors.ruleName && <p className="form__error">{fieldErrors.ruleName}</p>}
      </div>

      <div className="form__row">
        <label className="form__label" htmlFor="planScenario">
          시나리오명 (선택)
        </label>
        <input
          id="planScenario"
          className="form__input"
          value={form.scenarioName}
          onChange={updateField('scenarioName')}
        />
        {fieldErrors.scenarioName && <p className="form__error">{fieldErrors.scenarioName}</p>}
      </div>

      <div className="form__row">
        <label className="form__check" htmlFor="planGm">
          <input id="planGm" type="checkbox" checked={form.gm} onChange={updateField('gm')} />
          내가 GM(진행자)입니다
        </label>
      </div>

      <div className="form__row">
        <label className="form__label" htmlFor="planStatus">
          상태
        </label>
        <select
          id="planStatus"
          className="form__input"
          value={form.status}
          onChange={updateField('status')}
        >
          {STATUS_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>

      <div className="form__row">
        <label className="form__label" htmlFor="planMemo">
          메모 (선택)
        </label>
        <textarea
          id="planMemo"
          className="form__input form__textarea"
          placeholder="모집 여부, 장소, 준비물 등"
          value={form.memo}
          onChange={updateField('memo')}
        />
        {fieldErrors.memo && <p className="form__error">{fieldErrors.memo}</p>}
      </div>

      {error && <p className="text--error">{error}</p>}

      <div className="form__actions">
        <button className="button" type="submit" disabled={submitting}>
          {submitting ? '저장 중...' : submitLabel}
        </button>
        {onCancel && (
          <button className="button button--muted" type="button" onClick={onCancel}>
            취소
          </button>
        )}
      </div>
    </form>
  )
}

export default SessionForm
