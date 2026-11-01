import { useEffect, useMemo, useState } from 'react'

import { fetchRules } from '@/api/rules.js'
import { FALLBACK_RULE_SYSTEMS } from '@/constants/rules.js'

/** 한 번에 보여 줄 후보 수. 많으면 입력칸을 가린다. */
const MAX_SUGGESTIONS = 5

/**
 * 룰 입력칸. (감상·세션 일정 폼 공용)
 *
 * 이름 일부를 치면 후보를 보여 주고, **약어**(예: `coc`)를 치면 그 약어로 시작하는 룰을 찾아 준다.
 * 룰은 서버 목록에 없어도 자유 입력으로 저장할 수 있으므로, 후보는 "추천"일 뿐 강제하지 않는다.
 *
 * @param {object} props
 * @param {string} props.id 입력칸 id (라벨의 `htmlFor` 와 연결)
 * @param {string} props.value 현재 값(룰 이름)
 * @param {(value: string) => void} props.onChange 값을 고를 때 호출(이름 문자열을 넘긴다)
 */
function RuleInput({ id, value, onChange }) {
  const [rules, setRules] = useState(FALLBACK_RULE_SYSTEMS)

  // 서버 목록이 오면 그것으로 바꾼다. 실패해도 기본 목록으로 계속 쓸 수 있다.
  useEffect(() => {
    let ignore = false

    fetchRules().then((list) => {
      if (!ignore && list.length > 0) {
        setRules(list)
      }
    })

    return () => {
      ignore = true
    }
  }, [])

  const suggestions = useMemo(() => {
    const keyword = value.trim().toLowerCase()

    // 아직 아무것도 입력하지 않았거나, 이미 정확한 이름을 골랐으면 후보를 보여 줄 필요가 없다.
    if (!keyword || rules.some((rule) => rule.label === value)) {
      return []
    }

    return rules
      .filter(
        (rule) =>
          rule.label.toLowerCase().includes(keyword) || rule.code.toLowerCase().startsWith(keyword),
      )
      .slice(0, MAX_SUGGESTIONS)
  }, [rules, value])

  return (
    <div className="rule-input">
      <input
        id={id}
        className="form__input"
        list={`${id}-options`}
        placeholder="예: 크툴루의 부름 7판 (또는 COC7)"
        autoComplete="off"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      <datalist id={`${id}-options`}>
        {rules.map((rule) => (
          <option key={rule.code} value={rule.label} />
        ))}
      </datalist>

      {suggestions.length > 0 && (
        <ul className="rule-input__suggestions">
          {suggestions.map((rule) => (
            <li key={rule.code}>
              <button type="button" onClick={() => onChange(rule.label)}>
                <span className="rule-input__code">{rule.code}</span>
                {rule.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default RuleInput
