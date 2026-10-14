import { useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { createReview } from '@/api/reviews.js'
import SpoilerText from '@/components/SpoilerText.jsx'
import { DEFAULT_RULES } from '@/constants/rules.js'
import { FOLD_MARKER, SPOILER_MODES, VISIBILITIES } from '@/constants/spoilerMarkers.js'
import { REVIEW_TAGS } from '@/constants/tags.js'

/** 서버와 같은 규칙: 본문은 100자 이상. */
const MIN_BODY_LENGTH = 100

function ReviewNewPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const bodyRef = useRef(null)

  const [form, setForm] = useState({
    toHandle: searchParams.get('to') ?? '',
    playedOn: new Date().toISOString().slice(0, 10),
    ruleName: '',
    scenarioName: '',
    body: '',
    spoilerMode: 'NONE',
    visibility: 'PUBLIC',
  })
  const [tags, setTags] = useState([])
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  const updateField = (name) => (event) => {
    const { value } = event.target
    setForm((previous) => ({ ...previous, [name]: value }))
  }

  const toggleTag = (code) => {
    setTags((previous) =>
      previous.includes(code) ? previous.filter((item) => item !== code) : [...previous, code],
    )
  }

  /** 값이 반영된 다음 프레임에 커서를 옮긴다. */
  const moveCursor = (textarea, position) => {
    requestAnimationFrame(() => {
      textarea.focus()
      textarea.setSelectionRange(position, position)
    })
  }

  /** 선택한 글자를 ||...|| 로 감싼다. (스포일러 가리기) */
  const insertBlurMarker = () => {
    const textarea = bodyRef.current
    if (!textarea) {
      return
    }

    const { selectionStart, selectionEnd, value } = textarea
    const selected = value.slice(selectionStart, selectionEnd)
    const insert = `||${selected || '스포일러 내용'}||`
    const next = value.slice(0, selectionStart) + insert + value.slice(selectionEnd)

    setForm((previous) => ({ ...previous, body: next }))
    moveCursor(textarea, selectionStart + insert.length)
  }

  /** 커서 자리에 "여기서부터 접기" 표시를 넣는다. */
  const insertFoldMarker = () => {
    const textarea = bodyRef.current
    if (!textarea) {
      return
    }

    const { selectionStart, value } = textarea
    const insert = `\n${FOLD_MARKER}\n`
    const next = value.slice(0, selectionStart) + insert + value.slice(selectionStart)

    setForm((previous) => ({ ...previous, body: next }))
    moveCursor(textarea, selectionStart + insert.length)
  }

  const bodyLength = form.body.trim().length
  const tooShort = bodyLength < MIN_BODY_LENGTH

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)

    try {
      const created = await createReview({ ...form, tags })
      navigate(`/reviews/${created.id}`, { replace: true })
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
    <section className="page">
      <h1 className="page__title">감상 쓰기</h1>
      <p className="page__description">
        함께 플레이한 사람에게, 그 세션(1회 플레이)에 대한 감상을 남깁니다.
      </p>

      <form className="form" onSubmit={handleSubmit}>
        <div className="form__row">
          <label className="form__label" htmlFor="toHandle">
            받는 사람의 핸들 (필수)
          </label>
          <input
            id="toHandle"
            className="form__input"
            placeholder="예: godsse"
            value={form.toHandle}
            onChange={updateField('toHandle')}
          />
          {fieldErrors.toHandle && <p className="form__error">{fieldErrors.toHandle}</p>}
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="playedOn">
            플레이한 날짜 (필수)
          </label>
          <input
            id="playedOn"
            className="form__input"
            type="date"
            value={form.playedOn}
            onChange={updateField('playedOn')}
          />
          {fieldErrors.playedOn && <p className="form__error">{fieldErrors.playedOn}</p>}
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="ruleName">
            룰 (필수)
          </label>
          <input
            id="ruleName"
            className="form__input"
            list="rule-options"
            placeholder="예: 크툴루의 부름 7판"
            value={form.ruleName}
            onChange={updateField('ruleName')}
          />
          <datalist id="rule-options">
            {DEFAULT_RULES.map((rule) => (
              <option key={rule} value={rule} />
            ))}
          </datalist>
          {fieldErrors.ruleName && <p className="form__error">{fieldErrors.ruleName}</p>}
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="scenarioName">
            시나리오명 (선택)
          </label>
          <input
            id="scenarioName"
            className="form__input"
            value={form.scenarioName}
            onChange={updateField('scenarioName')}
          />
          {fieldErrors.scenarioName && <p className="form__error">{fieldErrors.scenarioName}</p>}
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="body">
            감상 내용 (필수, 100자 이상)
          </label>
          <div className="form__actions">
            <button className="button button--muted" type="button" onClick={insertBlurMarker}>
              선택한 글자 가리기
            </button>
            <button className="button button--muted" type="button" onClick={insertFoldMarker}>
              여기서부터 접기
            </button>
          </div>
          <textarea
            id="body"
            ref={bodyRef}
            className="form__input form__textarea form__textarea--tall"
            value={form.body}
            onChange={updateField('body')}
          />
          <p className={tooShort ? 'form__error' : 'form__hint'}>
            {bodyLength}자{tooShort ? ` (${MIN_BODY_LENGTH - bodyLength}자 더 필요합니다)` : ''}
          </p>
          {fieldErrors.body && <p className="form__error">{fieldErrors.body}</p>}

          {form.body && (
            <div className="card">
              <h2 className="card__title">미리보기 (스포일러 처리 확인)</h2>
              <SpoilerText spoilerMode={form.spoilerMode} text={form.body} />
            </div>
          )}
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="spoilerMode">
            스포일러 표시
          </label>
          <select
            id="spoilerMode"
            className="form__input"
            value={form.spoilerMode}
            onChange={updateField('spoilerMode')}
          >
            {SPOILER_MODES.map((mode) => (
              <option key={mode.value} value={mode.value}>
                {mode.label}
              </option>
            ))}
          </select>
          <p className="form__hint">
            본문 일부만 가리려면, 가릴 문장을 선택한 뒤 ‘선택한 글자 가리기’ 버튼을 누르세요.
          </p>
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="visibility">
            공개 범위
          </label>
          <select
            id="visibility"
            className="form__input"
            value={form.visibility}
            onChange={updateField('visibility')}
          >
            {VISIBILITIES.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </div>

        <div className="form__row">
          <span className="form__label">감정 태그 (선택, 최대 8개)</span>
          <div className="tags">
            {REVIEW_TAGS.map((tag) => (
              <button
                className={tags.includes(tag.code) ? 'chip chip--active' : 'chip'}
                key={tag.code}
                type="button"
                onClick={() => toggleTag(tag.code)}
              >
                {tag.label}
              </button>
            ))}
          </div>
        </div>

        {error && <p className="text--error">{error}</p>}

        <div className="form__actions">
          <button className="button" type="submit" disabled={submitting || tooShort}>
            {submitting ? '올리는 중...' : '감상 남기기'}
          </button>
        </div>
      </form>
    </section>
  )
}

export default ReviewNewPage
