package com.godsse.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.RuleSystem;

/**
 * 룰(규칙 체계) 목록 조회.
 */
public interface RuleSystemRepository extends JpaRepository<RuleSystem, Long> {

	/** 자동완성에 사용할 활성 룰 목록(정렬 순서대로). */
	List<RuleSystem> findByActiveTrueOrderBySortOrderAsc();

	Optional<RuleSystem> findByCode(String code);

}
