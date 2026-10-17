package com.godsse.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.SessionPlan;

/**
 * 세션 일정(달력) 조회/저장.
 *
 * <p>
 * 달력은 한 달치를 한 번에 그리므로 페이지 대신 <b>기간 조건 목록</b>으로 가져온다.
 * 정렬은 날짜 → id 순(같은 날 여러 일정이면 만든 순서대로).
 */
public interface SessionPlanRepository extends JpaRepository<SessionPlan, Long> {

	/** 한 달치 달력에 표시할 일정(기간 조건). */
	List<SessionPlan> findByOwnerIdAndPlannedDateBetweenOrderByPlannedDateAscIdAsc(Long ownerId, LocalDate from,
			LocalDate to);

}
