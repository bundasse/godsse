package com.godsse.backend.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.godsse.backend.domain.SessionPlan;
import com.godsse.backend.domain.enums.SessionStatus;

/**
 * 세션 일정 응답(달력에 표시할 한 건).
 *
 * @param id           일정 번호
 * @param title        제목
 * @param plannedDate  날짜
 * @param ruleName     룰
 * @param scenarioName 시나리오명(없으면 null)
 * @param gm           내가 GM 인지
 * @param memo         메모(없으면 null)
 * @param status       상태(PLANNED / DONE / CANCELED)
 * @param createdAt    작성 시각
 */
public record SessionPlanResponse(Long id, String title, LocalDate plannedDate, String ruleName, String scenarioName,
		boolean gm, String memo, SessionStatus status, Instant createdAt) {

	public static SessionPlanResponse of(SessionPlan plan) {
		return new SessionPlanResponse(plan.getId(), plan.getTitle(), plan.getPlannedDate(), plan.getRuleName(),
				plan.getScenarioName(), plan.isGm(), plan.getMemo(), plan.getStatus(), plan.getCreatedAt());
	}

}
