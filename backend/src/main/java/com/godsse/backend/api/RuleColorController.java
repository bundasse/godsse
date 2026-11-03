package com.godsse.backend.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.godsse.backend.api.dto.RuleColorRequest;
import com.godsse.backend.api.dto.RuleColorResponse;
import com.godsse.backend.common.LoginUser;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.RuleColorService;

import jakarta.validation.Valid;

/**
 * 히트맵 룰 색(개인화) API. 모두 로그인이 필요하고, 내 색만 다룬다.
 *
 * <p>
 * 룰 이름을 경로가 아니라 본문·쿼리로 받는 이유: 룰은 자유 입력이라 이름에 {@code /} 같은
 * 주소 문자를 넣을 수 있다. 경로 변수로 받으면 그런 이름이 잘려 엉뚱한 색이 저장된다.
 */
@RestController
@RequestMapping("/api/users/me/rule-colors")
public class RuleColorController {

	private final RuleColorService ruleColorService;

	public RuleColorController(RuleColorService ruleColorService) {
		this.ruleColorService = ruleColorService;
	}

	/** 내가 바꾼 색 목록(기본 팔레트 색은 저장하지 않으므로 빠진다). */
	@GetMapping
	public List<RuleColorResponse> list(@LoginUser User user) {
		return this.ruleColorService.list(user);
	}

	/**
	 * 룰 색 저장(업서트). 본문 예: {@code {"ruleName":"던전월드","color":"#16a34a"}}
	 *
	 * <p>
	 * 색은 소문자로 맞춰 저장하므로 {@code #16A34A} 를 보내도 {@code #16a34a} 로 돌아온다.
	 */
	@PutMapping
	public RuleColorResponse save(@LoginUser User user, @Valid @RequestBody RuleColorRequest request) {
		return this.ruleColorService.save(user, request);
	}

	/**
	 * 룰 색을 지워 기본 팔레트 색으로 되돌린다. 성공하면 204.
	 *
	 * <p>
	 * 예: {@code DELETE /api/users/me/rule-colors?ruleName=던전월드}
	 */
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reset(@LoginUser User user, @RequestParam(name = "ruleName", required = false) String ruleName) {
		this.ruleColorService.reset(user, ruleName);
	}

}
