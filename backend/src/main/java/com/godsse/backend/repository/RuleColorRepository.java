package com.godsse.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.RuleColor;

/**
 * 룰 색(히트맵 개인화) 조회/저장.
 *
 * <p>
 * 한 사람의 색은 많아야 \"룰 종류 수\" 만큼이라 페이지네이션 없이 전부 가져온다.
 */
public interface RuleColorRepository extends JpaRepository<RuleColor, Long> {

	/** 내가 바꾼 색 전부(히트맵을 그릴 때 한 번에 읽는다). */
	List<RuleColor> findByUserId(Long userId);

	/** 특정 룰에 지정한 색. 없으면 기본 팔레트 색을 쓴다. */
	Optional<RuleColor> findByUserIdAndRuleName(Long userId, String ruleName);

}
