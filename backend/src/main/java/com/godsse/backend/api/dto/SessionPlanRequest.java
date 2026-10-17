package com.godsse.backend.api.dto;

import java.time.LocalDate;

import com.godsse.backend.domain.enums.SessionStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /api/sessions} · {@code PATCH /api/sessions/{id}} 요청 — 세션 일정 작성·수정.
 *
 * <p>
 * 필수: 제목·날짜·룰. 선택: 시나리오명·GM 여부·메모.
 *
 * @param title        일정 제목(예: 정기 캠페인 3화)
 * @param plannedDate  예정(또는 플레이한) 날짜 — 미래·과거 모두 가능
 * @param ruleName     사용할 룰
 * @param scenarioName 시나리오명(선택)
 * @param gm           내가 GM 인지 (생략하면 false)
 * @param memo         메모(모집 여부·장소 등, 선택)
 * @param status       상태. 작성 시 생략하면 {@code PLANNED}, 수정 시 생략하면 기존 상태 유지
 */
public record SessionPlanRequest(@NotBlank(message = "일정 제목은 필수입니다.")
		@Size(max = 60, message = "제목은 60자 이하여야 합니다.") String title,

		@NotNull(message = "날짜는 필수입니다.") LocalDate plannedDate,

		@NotBlank(message = "룰은 필수입니다.")
		@Size(max = 50, message = "룰은 50자 이하여야 합니다.") String ruleName,

		@Size(max = 100, message = "시나리오명은 100자 이하여야 합니다.") String scenarioName,

		// primitive boolean 이 아니라 Boolean(래퍼)을 쓴다.
		// Jackson 은 record 를 만들 때 "값이 없는 primitive" 를 채우지 못해 400 을 낸다.
		Boolean gm,

		@Size(max = 1000, message = "메모는 1000자 이하여야 합니다.") String memo,

		SessionStatus status) {
}
