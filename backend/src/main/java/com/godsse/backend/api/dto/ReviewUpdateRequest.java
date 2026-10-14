package com.godsse.backend.api.dto;

import java.time.LocalDate;
import java.util.Set;

import com.godsse.backend.domain.enums.SpoilerMode;
import com.godsse.backend.domain.enums.Visibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * {@code PATCH /api/reviews/{id}} 요청 — 감상 수정.
 *
 * <p>
 * 작성·수정 공용 DTO 를 쓰지 않고 따로 둔 이유: <b>받는 사람은 바꿀 수 없기</b> 때문이다.
 * ({@code toHandle} 이 없다)
 *
 * @param playedOn     플레이한 날짜
 * @param ruleName     사용한 룰
 * @param scenarioName 시나리오명(선택)
 * @param body         본문(100자 이상)
 * @param spoilerMode  스포일러 표시 방식(없으면 {@code NONE})
 * @param visibility   공개 범위(없으면 {@code PUBLIC})
 * @param tags         감정 태그(최대 8개)
 */
public record ReviewUpdateRequest(

		@NotNull(message = "플레이 날짜는 필수입니다.")
		@PastOrPresent(message = "플레이 날짜는 오늘까지만 고를 수 있습니다.") LocalDate playedOn,

		@NotBlank(message = "룰은 필수입니다.")
		@Size(max = 50, message = "룰은 50자 이하여야 합니다.") String ruleName,

		@Size(max = 100, message = "시나리오명은 100자 이하여야 합니다.") String scenarioName,

		@NotBlank(message = "감상 내용은 필수입니다.")
		@Size(min = 100, max = 10000, message = "감상은 100자 이상 10000자 이하로 써 주세요.") String body,

		SpoilerMode spoilerMode,

		Visibility visibility,

		@Size(max = 8, message = "태그는 8개까지 고를 수 있습니다.") Set<String> tags) {
}
