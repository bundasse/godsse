import { useState } from 'react'

import { BLUR_MARKER, FOLD_MARKER } from '@/constants/spoilerMarkers.js'

/** `||` 마커 개수가 짝수일 때만 마커로 인정한다. 짝이 안 맞으면 null(원문 그대로 보여 준다). */
function splitByBlur(text) {
  const parts = text.split(BLUR_MARKER)
  return parts.length % 2 === 1 ? parts : null
}

/** 첫 번째 `:::spoiler` 를 기준으로 앞/뒤를 나눈다. */
function splitByFold(text) {
  const index = text.indexOf(FOLD_MARKER)
  if (index < 0) {
    return { head: text, tail: null }
  }
  return {
    head: text.slice(0, index).replace(/\s+$/, ''),
    tail: text.slice(index + FOLD_MARKER.length).replace(/^\s+/, ''),
  }
}

/** 가려진 조각. interactive 가 true 일 때만 눌러서 볼 수 있다. */
function BlurPiece({ text, interactive }) {
  const [shown, setShown] = useState(false)

  if (!interactive) {
    return <span className="spoiler spoiler--hidden">{text}</span>
  }

  const toggle = () => setShown((previous) => !previous)

  return (
    <span
      className={shown ? 'spoiler' : 'spoiler spoiler--hidden'}
      role="button"
      tabIndex={0}
      title={shown ? '다시 가리기' : '눌러서 보기'}
      onClick={toggle}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          toggle()
        }
      }}
    >
      {text}
    </span>
  )
}

/** `||` 로 나눈 조각들을 렌더링한다. 홀수 번째 조각이 가려진 부분이다. */
function InlineText({ text, interactive }) {
  const parts = splitByBlur(text)

  if (!parts) {
    return text
  }

  return parts.map((part, index) =>
    index % 2 === 0 ? (
      <span key={index}>{part}</span>
    ) : (
      <BlurPiece key={index} interactive={interactive} text={part} />
    ),
  )
}

/** `:::spoiler` 이후 내용. interactive 일 때만 펼칠 수 있다. */
function FoldPart({ text, interactive }) {
  const [open, setOpen] = useState(false)

  if (!interactive) {
    return <p className="text--muted">스포일러가 있어 접어 두었습니다. 눌러서 전문을 보세요.</p>
  }

  if (open) {
    return (
      <p className="spoiler-text__body">
        <InlineText interactive text={text} />
      </p>
    )
  }

  return (
    <button className="button button--muted" type="button" onClick={() => setOpen(true)}>
      스포일러 펼치기
    </button>
  )
}

/**
 * 감상 본문을 스포일러 규칙에 맞게 보여 준다.
 *
 * <ul>
 * <li>{@code ||문장||} — 그 문장만 가리고, 누르면 보인다</li>
 * <li>단독 줄 {@code :::spoiler} — 그 지점 이후를 접는다</li>
 * <li>{@code spoilerMode} 가 BLUR/FOLD 면 본문 전체를 가리거나 접는다</li>
 * </ul>
 *
 * 짝이 맞지 않는 마커는 자르지 않고 그대로 보여 준다. (서버의 미리보기 만들기와 같은 규칙)
 *
 * @param {object} props
 * @param {string} props.text 본문(또는 미리보기)
 * @param {'NONE'|'BLUR'|'FOLD'} [props.spoilerMode] 작성자가 정한 전체 스포일러 표시 방식
 * @param {boolean} [props.interactive] 눌러서 볼 수 있게 할지(카드 안에서는 false — 링크 이동과 겹치지 않도록)
 */
function SpoilerText({ text, spoilerMode = 'NONE', interactive = true }) {
  const [revealed, setRevealed] = useState(false)

  if (!text) {
    return null
  }

  const { head, tail } = splitByFold(text)
  const wholeHidden = spoilerMode !== 'NONE' && !revealed

  if (wholeHidden) {
    return (
      <div className="spoiler-text">
        {spoilerMode === 'BLUR' ? (
          <p
            className={
              interactive
                ? 'spoiler-text__body spoiler-text__body--blurred'
                : 'spoiler-text__body spoiler--hidden'
            }
            onClick={interactive ? () => setRevealed(true) : undefined}
          >
            {head}
          </p>
        ) : (
          <p className="text--muted">스포일러가 포함되어 있습니다. 눌러서 내용을 보세요.</p>
        )}

        {interactive && (
          <button className="button button--muted" type="button" onClick={() => setRevealed(true)}>
            내용 보기
          </button>
        )}
      </div>
    )
  }

  return (
    <div className="spoiler-text">
      <p className="spoiler-text__body">
        <InlineText interactive={interactive} text={head} />
      </p>
      {tail !== null && <FoldPart interactive={interactive} text={tail} />}
    </div>
  )
}

export default SpoilerText
