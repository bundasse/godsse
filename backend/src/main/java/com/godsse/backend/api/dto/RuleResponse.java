package com.godsse.backend.api.dto;

import com.godsse.backend.domain.RuleSystem;

/**
 * 룰(규칙 체계) 한 건 — 자동완성 선택지.
 *
 * @param code  약어(예: {@code COC7}). 화면에서 약어만 쳐도 찾을 수 있게 내려준다
 * @param label 화면에 보이는 이름(예: {@code 크툴루의 부름 7판})
 */
public record RuleResponse(String code, String label) {

	public static RuleResponse of(RuleSystem rule) {
		return new RuleResponse(rule.getCode(), rule.getLabel());
	}

}
