package com.godsse.backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.SessionPlanRequest;
import com.godsse.backend.api.dto.SessionPlanResponse;
import com.godsse.backend.api.dto.UserSummary;
import com.godsse.backend.common.exception.BadRequestException;
import com.godsse.backend.common.exception.ConflictException;
import com.godsse.backend.common.exception.ForbiddenException;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.domain.SessionParticipant;
import com.godsse.backend.domain.SessionPlan;
import com.godsse.backend.domain.User;
import com.godsse.backend.repository.SessionParticipantRepository;
import com.godsse.backend.repository.SessionPlanRepository;
import com.godsse.backend.repository.UserRepository;

/**
 * 세션 일정(달력) 작성·조회·수정·삭제 + 함께한 사람 기록.
 *
 * <p>
 * 일정은 <b>나만 보는 계획표</b>이므로 다른 사람의 일정은 조회·수정할 수 없다.
 * (감상과 달리 공개 범위 개념이 없다)
 *
 * <p>
 * 함께한 사람은 <b>가입한 사용자</b>만 기록한다. 나중에 그 사람에게 감상을 남기러 갈 때
 * 핸들이 필요하기 때문이다(자유 입력 이름은 프로필로 이어 줄 수 없다).
 */
@Service
@Transactional(readOnly = true)
public class SessionPlanService {

	/** 한 번에 조회할 수 있는 최대 기간(일). 실수로 전체 기간을 요청하는 것을 막는다. */
	private static final long MAX_RANGE_DAYS = 62;

	private final SessionPlanRepository sessionPlanRepository;
	private final SessionParticipantRepository sessionParticipantRepository;
	private final UserRepository userRepository;

	public SessionPlanService(SessionPlanRepository sessionPlanRepository,
			SessionParticipantRepository sessionParticipantRepository, UserRepository userRepository) {
		this.sessionPlanRepository = sessionPlanRepository;
		this.sessionParticipantRepository = sessionParticipantRepository;
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

		return toResponses(this.sessionPlanRepository
			.findByOwnerIdAndPlannedDateBetweenOrderByPlannedDateAscIdAsc(loginUser.getId(), from, to));
	}

	/** 일정을 만든다. 상태는 항상 {@code PLANNED} 로 시작한다. */
	@Transactional
	public SessionPlanResponse create(User loginUser, SessionPlanRequest request) {
		User owner = this.userRepository.findById(loginUser.getId())
			.orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

		SessionPlan plan = new SessionPlan(owner, request.title().trim(), request.plannedDate(),
				request.ruleName().trim(), trimToNull(request.scenarioName()), Boolean.TRUE.equals(request.gm()),
				trimToNull(request.memo()));

		return SessionPlanResponse.of(this.sessionPlanRepository.save(plan), List.of());
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

		return SessionPlanResponse.of(plan, participantsOf(plan.getId()));
	}

	/** 일정을 삭제한다. 함께한 사람 행을 먼저 지워야 FK 제약에 걸리지 않는다. */
	@Transactional
	public void delete(User loginUser, Long id) {
		SessionPlan plan = getOwnedPlan(loginUser, id);

		this.sessionParticipantRepository.deleteByPlanId(plan.getId());
		this.sessionPlanRepository.delete(plan);
	}

	/**
	 * 일정에 함께한 사람을 추가한다.
	 *
	 * <p>
	 * 이미 있는 사람은 409, 나 자신은 400, 없는 핸들은 404 로 알려 준다.
	 */
	@Transactional
	public void addParticipant(User loginUser, Long id, String handle) {
		SessionPlan plan = getOwnedPlan(loginUser, id);
		User participant = getUserByHandle(handle);

		if (participant.getId().equals(loginUser.getId())) {
			throw new BadRequestException("handle", "나 자신은 참가자로 넣을 수 없습니다.");
		}
		if (this.sessionParticipantRepository.existsByPlanIdAndUserId(plan.getId(), participant.getId())) {
			throw new ConflictException("이미 참가자로 기록된 사람입니다.");
		}

		this.sessionParticipantRepository.save(new SessionParticipant(plan, participant));
	}

	/** 일정에서 함께한 사람을 뺀다. */
	@Transactional
	public void removeParticipant(User loginUser, Long id, String handle) {
		SessionPlan plan = getOwnedPlan(loginUser, id);
		User participant = getUserByHandle(handle);

		SessionParticipant link = this.sessionParticipantRepository
			.findByPlanIdAndUserId(plan.getId(), participant.getId())
			.orElseThrow(() -> new NotFoundException("참가자를 찾을 수 없습니다."));

		this.sessionParticipantRepository.delete(link);
	}

	/**
	 * 일정 목록을 응답으로 바꾼다.
	 *
	 * <p>
	 * 참가자는 <b>한 번에</b> 읽어 일정별로 묶는다. 일정마다 조회하면 달력 한 달치를 그릴 때
	 * 일정 수만큼 쿼리가 나간다(N+1).
	 */
	private List<SessionPlanResponse> toResponses(List<SessionPlan> plans) {
		if (plans.isEmpty()) {
			return List.of();
		}

		List<Long> planIds = plans.stream().map(SessionPlan::getId).toList();
		Map<Long, List<UserSummary>> participantsByPlan = new HashMap<>();

		for (SessionParticipant participant : this.sessionParticipantRepository.findByPlanIdsWithUser(planIds)) {
			participantsByPlan.computeIfAbsent(participant.getPlan().getId(), key -> new ArrayList<>())
				.add(UserSummary.of(participant.getUser()));
		}

		return plans.stream()
			.map(plan -> SessionPlanResponse.of(plan, participantsByPlan.getOrDefault(plan.getId(), List.of())))
			.toList();
	}

	/** 한 일정의 참가자 목록(추가·수정 응답용). */
	private List<UserSummary> participantsOf(Long planId) {
		return this.sessionParticipantRepository.findByPlanIdWithUser(planId)
			.stream()
			.map(participant -> UserSummary.of(participant.getUser()))
			.toList();
	}

	/** 핸들로 사용자를 찾는다. 없으면 404. */
	private User getUserByHandle(String handle) {
		String trimmed = handle == null ? "" : handle.trim();

		return this.userRepository.findByHandle(trimmed)
			.orElseThrow(() -> new NotFoundException("handle", "해당 핸들의 사용자를 찾을 수 없습니다."));
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
