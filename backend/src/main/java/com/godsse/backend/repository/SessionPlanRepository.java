package com.godsse.backend.repository;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.SessionPlan;

/**
 * 세션 일정(달력) 조회/저장.
 */
public interface SessionPlanRepository extends JpaRepository<SessionPlan, Long> {

	/** 한 달치 달력에 표시할 일정(기간 조건). */
	Page<SessionPlan> findByOwnerIdAndPlannedDateBetween(Long ownerId, LocalDate from, LocalDate to,
			Pageable pageable);

	/** 내 일정 전체(최근 날짜부터). */
	Page<SessionPlan> findByOwnerIdOrderByPlannedDateDesc(Long ownerId, Pageable pageable);

}
