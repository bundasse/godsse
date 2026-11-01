package com.godsse.backend.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.godsse.backend.api.dto.RuleResponse;
import com.godsse.backend.service.RuleService;

/**
 * 룰(규칙 체계) 목록 API.
 *
 * <p>
 * 감상·일정 작성 화면의 자동완성에 쓰는 <b>참조 데이터</b>라 로그인 없이 볼 수 있다.
 * (개인 정보가 아니고, 로그인 전에도 화면을 그릴 수 있어야 한다)
 */
@RestController
@RequestMapping("/api/rules")
public class RuleController {

	private final RuleService ruleService;

	public RuleController(RuleService ruleService) {
		this.ruleService = ruleService;
	}

	/** 활성 룰 목록(정렬 순서대로). */
	@GetMapping
	public List<RuleResponse> list() {
		return this.ruleService.list();
	}

}
