package com.godsse.backend.api.dto;

import com.godsse.backend.domain.RuleColor;

/**
 * 내가 지정한 룰 색 한 건.
 *
 * @param ruleName 룰 이름
 * @param color    저장된 색({@code #rrggbb}, 소문자)
 */
public record RuleColorResponse(String ruleName, String color) {

	public static RuleColorResponse of(RuleColor ruleColor) {
		return new RuleColorResponse(ruleColor.getRuleName(), ruleColor.getColor());
	}

}
