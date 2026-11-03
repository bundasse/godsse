package com.godsse.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * {@code PUT /api/users/me/rule-colors} 요청 — 룰 색 저장(업서트).
 *
 * <p>
 * 룰 이름을 <b>경로가 아니라 본문</b>으로 받는다. 룰은 자유 입력이라 이름에 {@code /} 같은
 * 주소 문자를 넣을 수 있는데, 경로 변수({@code /{ruleName}})로 받으면 그런 이름이 잘린다.
 *
 * @param ruleName 색을 바꿀 룰 이름(일정에 저장된 문자열 그대로)
 * @param color    {@code #RRGGBB} 형식의 색
 */
public record RuleColorRequest(@NotBlank(message = "룰 이름은 필수입니다.")
@Size(max = 50, message = "룰 이름은 50자 이하여야 합니다.") String ruleName,

		@NotBlank(message = "색상은 필수입니다.")
		@Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "색상은 #RRGGBB 형식이어야 합니다.") String color) {
}
