package com.godsse.backend.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.godsse.backend.api.dto.SessionParticipantRequest;
import com.godsse.backend.api.dto.SessionPlanRequest;
import com.godsse.backend.api.dto.SessionPlanResponse;
import com.godsse.backend.common.LoginUser;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.SessionPlanService;

import jakarta.validation.Valid;

/**
 * 세션 일정(달력) API. 모두 로그인이 필요하다(내 일정만 다룬다).
 *
 * <p>
 * 달력은 한 달치를 한 번에 그리므로 목록은 기간(`from`~`to`)으로 받고 페이지네이션을 쓰지 않는다.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionPlanController {

	private final SessionPlanService sessionPlanService;

	public SessionPlanController(SessionPlanService sessionPlanService) {
		this.sessionPlanService = sessionPlanService;
	}

	/**
	 * 기간 내 내 일정 목록. 예: {@code GET /api/sessions?from=2026-10-01&to=2026-10-31}
	 */
	@GetMapping
	public List<SessionPlanResponse> list(@LoginUser User user,
			@RequestParam(name = "from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(name = "to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return this.sessionPlanService.list(user, from, to);
	}

	/** 일정 작성. 성공하면 201. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SessionPlanResponse create(@LoginUser User user, @Valid @RequestBody SessionPlanRequest request) {
		return this.sessionPlanService.create(user, request);
	}

	/** 일정 수정(상태 변경 포함). 작성자(소유자)만 가능하다. */
	@PatchMapping("/{id}")
	public SessionPlanResponse update(@LoginUser User user, @PathVariable Long id,
			@Valid @RequestBody SessionPlanRequest request) {
		return this.sessionPlanService.update(user, id, request);
	}

	/** 일정 삭제. 소유자만 가능하다. 성공하면 204. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@LoginUser User user, @PathVariable Long id) {
		this.sessionPlanService.delete(user, id);
	}

	/**
	 * 일정에 함께한 사람을 추가한다(소유자만). 성공하면 204.
	 *
	 * <p>
	 * 본문 예: {@code {"handle":"godssefan"}}
	 */
	@PostMapping("/{id}/participants")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void addParticipant(@LoginUser User user, @PathVariable Long id,
			@Valid @RequestBody SessionParticipantRequest request) {
		this.sessionPlanService.addParticipant(user, id, request.handle());
	}

	/** 일정에서 함께한 사람을 뺀다(소유자만). 성공하면 204. */
	@DeleteMapping("/{id}/participants/{handle}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeParticipant(@LoginUser User user, @PathVariable Long id, @PathVariable String handle) {
		this.sessionPlanService.removeParticipant(user, id, handle);
	}

}
