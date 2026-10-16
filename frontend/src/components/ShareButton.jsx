import { useState } from 'react'

/**
 * 링크를 클립보드에 복사하는 버튼.
 *
 * 클립보드 API 는 https 또는 localhost 에서만 동작하므로, 실패하면 주소를 보여 주고
 * 직접 복사하게 안내한다.
 *
 * @param {object} props
 * @param {string} props.url 복사할 주소
 * @param {string} [props.label] 버튼 문구
 */
function ShareButton({ url, label = '링크 복사' }) {
  const [copied, setCopied] = useState(false)

  const handleClick = async () => {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      window.prompt('아래 주소를 복사해 주세요.', url)
    }
  }

  return (
    <button className="button button--muted" type="button" onClick={handleClick}>
      {copied ? '복사했습니다!' : label}
    </button>
  )
}

export default ShareButton
