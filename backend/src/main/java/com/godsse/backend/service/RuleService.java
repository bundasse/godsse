package com.godsse.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.RuleResponse;
import com.godsse.backend.repository.RuleSystemRepository;

/**
 * 룰(규칙 체계) 목록 조회.
 *
 * <p>
 * 룰 이름은 감상·일정에 <b>문자열로</b> 저장되므로 이 목록은 "정답"이 아니라 <b>추천</b>이다.
 * (목록에 없는 룰도 자유 입력으로 저장할 수 있다)
 */
@Service
@Transactional(readOnly = true)
public class RuleService {

	private final RuleSystemRepository ruleSystemRepository;

	public RuleService(RuleSystemRepository ruleSystemRepository) {
		this.ruleSystemRepository = ruleSystemRepository;
	}

	/** 자동완성용 활성 룰 목록(정렬 순서대로). */
	public List<RuleResponse> list() {
		return this.ruleSystemRepository.findByActiveTrueOrderBySortOrderAsc()
			.stream()
			.map(RuleResponse::of)
			.toList();
	}

}
