package com.godsse.backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.SessionPlanRequest;
import com.godsse.backend.api.dto.SessionPlanResponse;
import com.godsse.backend.common.exception.BadRequestException;
import com.godsse.backend.common.exception.ForbiddenException;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.domain.SessionPlan;
import com.godsse.backend.domain.User;
import com.godsse.backend.repository.SessionPlanRepository;
import com.godsse.backend.repository.UserRepository;

/**
 * 세션 일정(달력) 작성·조회·수정·삭제.
 *
 * <p>
 * 일정은 <b>나만 보는 계획표</b>이므로 다른 사람의 일정은 조회·수정할 수 없다.
 * (감상과 달리 공개 범위 개념이 없다)
 */
@Service
@Transactional(readOnly = true)
public class SessionPlanService {

	/** 한 번에 조회할 수 있는 최대 기간(일). 실수로 전체 기간을 요청하는 것을 막는다. */
	private static final long MAX_RANGE_DAYS = 62;

	private final SessionPlanRepository sessionPlanRepository;
	private final UserRepository userRepository;

	public SessionPlanService(SessionPlanRepository sessionPlanRepository, UserRepository userRepository) {
		this.sessionPlanRepository = sessionPlanRepository;
		this.userRepository = userRepository;
	}

	/**
	 * 기간 내 내 일정 목록(달력용).
	 *
	 * @throws BadRequestException 기간이 잘못되었거나 너무 길 때 (400)
	 */
	public List<SessionPlanResponse> list(User loginUser, LocalDate from, LocalDate to) {
		if (from == null || to == null || to.isBefore(from)) {
			throw new BadRequestException("from", "조회 기간이 올바르지 않습니다.");
		}
		if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
			throw new BadRequestException("to", "한 번에 " + MAX_RANGE_DAYS + "일까지만 조회할 수 있습니다.");
		}

		return this.sessionPlanRepository
			.findByOwnerIdAndPlannedDateBetweenOrderByPlannedDateAscIdAsc(loginUser.getId(), from, to)
			.stream()
			.map(SessionPlanResponse::of)
			.toList();
	}

	/** 일정을 만든다. 상태는 항상 {@code PLANNED} 로 시작한다. */
	@Transactional
	public SessionPlanResponse create(User loginUser, SessionPlanRequest request) {
		User owner = this.userRepository.findById(loginUser.getId())
			.orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

		SessionPlan plan = new SessionPlan(owner, request.title().trim(), request.plannedDate(),
				request.ruleName().trim(), trimToNull(request.scenarioName()), Boolean.TRUE.equals(request.gm()),
				trimToNull(request.memo()));

		return SessionPlanResponse.of(this.sessionPlanRepository.save(plan));
	}

	/**
	 * 일정을 수정한다. 상태를 함께 보내면 상태도 바꾼다(예: 예정 → 완료).
	 */
	@Transactional
	public SessionPlanResponse update(User loginUser, Long id, SessionPlanRequest request) {
		SessionPlan plan = getOwnedPlan(loginUser, id);

		// gm 을 생략하면 기존 값을 유지한다.
		boolean gm = request.gm() != null ? request.gm() : plan.isGm();

		plan.apply(request.title().trim(), request.plannedDate(), request.ruleName().trim(),
				trimToNull(request.scenarioName()), gm, trimToNull(request.memo()));

		if (request.status() != null) {
			plan.changeStatus(request.status());
		}

		return SessionPlanResponse.of(plan);
	}

	/** 일정을 삭제한다. */
	@Transactional
	public void delete(User loginUser, Long id) {
		this.sessionPlanRepository.delete(getOwnedPlan(loginUser, id));
	}

	/** 내 일정인지 확인하고 가져온다. */
	private SessionPlan getOwnedPlan(User loginUser, Long id) {
		SessionPlan plan = this.sessionPlanRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다."));

		if (!plan.getOwner().getId().equals(loginUser.getId())) {
			throw new ForbiddenException("내 일정만 수정하거나 삭제할 수 있습니다.");
		}

		return plan;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
