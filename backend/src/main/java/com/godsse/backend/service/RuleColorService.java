package com.godsse.backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.RuleColorRequest;
import com.godsse.backend.api.dto.RuleColorResponse;
import com.godsse.backend.common.RuleColors;
import com.godsse.backend.common.exception.BadRequestException;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.domain.RuleColor;
import com.godsse.backend.domain.User;
import com.godsse.backend.repository.RuleColorRepository;
import com.godsse.backend.repository.UserRepository;

/**
 * 룰 색(히트맵 개인화) 조회·저장.
 *
 * <p>
 * 룰 이름은 자유 입력 문자열이므로 색도 \"룰 + 사용자\" 조합으로 저장한다.
 * 사용자가 고른 색만 남기고, 나머지는 기본 팔레트 색을 계산해 쓴다
 * ({@link RuleColors#defaultColor(String)}). 그래서 같은 룰이라도 사람마다 다른 색을 쓸 수 있다.
 */
@Service
@Transactional(readOnly = true)
public class RuleColorService {

	/** 룰 이름 최대 길이({@code session_plans.rule_name} 과 같은 50자). */
	private static final int MAX_RULE_NAME_LENGTH = 50;

	private final RuleColorRepository ruleColorRepository;
	private final UserRepository userRepository;

	public RuleColorService(RuleColorRepository ruleColorRepository, UserRepository userRepository) {
		this.ruleColorRepository = ruleColorRepository;
		this.userRepository = userRepository;
	}

	/** 내가 바꾼 색만 돌려준다(기본 팔레트 색은 저장하지 않으므로 목록에 없다). */
	public List<RuleColorResponse> list(User loginUser) {
		return this.ruleColorRepository.findByUserId(loginUser.getId())
			.stream()
			.map(RuleColorResponse::of)
			.toList();
	}

	/**
	 * 색을 저장한다. 같은 룰에 이미 색이 있으면 바꾸고, 없으면 새로 만든다(업서트).
	 *
	 * @throws BadRequestException 룰 이름이 비었거나 색 형식이 틀렸을 때 (400)
	 */
	@Transactional
	public RuleColorResponse save(User loginUser, RuleColorRequest request) {
		String ruleName = normalizeRuleName(request.ruleName());
		String color = normalizeColor(request.color());

		RuleColor saved = this.ruleColorRepository.findByUserIdAndRuleName(loginUser.getId(), ruleName)
			.map(existing -> {
				existing.changeColor(color);
				return existing;
			})
			.orElseGet(() -> this.ruleColorRepository.save(new RuleColor(ownerOf(loginUser), ruleName, color)));

		return RuleColorResponse.of(saved);
	}

	/**
	 * 색을 지워 기본 팔레트 색으로 되돌린다.
	 *
	 * <p>
	 * 지정한 색이 없어도 성공으로 본다(같은 요청을 두 번 보내도 결과가 같다 — 멱등).
	 */
	@Transactional
	public void reset(User loginUser, String ruleName) {
		this.ruleColorRepository.findByUserIdAndRuleName(loginUser.getId(), normalizeRuleName(ruleName))
			.ifPresent(this.ruleColorRepository::delete);
	}

	/** 저장할 때는 실제 사용자 엔티티가 필요하다(세션 객체는 요청이 끝나면 분리된다). */
	private User ownerOf(User loginUser) {
		return this.userRepository.findById(loginUser.getId())
			.orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
	}

	private static String normalizeRuleName(String ruleName) {
		String trimmed = ruleName == null ? "" : ruleName.trim();
		if (trimmed.isEmpty()) {
			throw new BadRequestException("ruleName", "룰 이름은 필수입니다.");
		}
		if (trimmed.length() > MAX_RULE_NAME_LENGTH) {
			throw new BadRequestException("ruleName", "룰 이름은 " + MAX_RULE_NAME_LENGTH + "자 이하여야 합니다.");
		}
		return trimmed;
	}

	private static String normalizeColor(String color) {
		String trimmed = color == null ? "" : color.trim();
		if (!RuleColors.isValid(trimmed)) {
			throw new BadRequestException("color", "색상은 #RRGGBB 형식이어야 합니다.");
		}
		return trimmed.toLowerCase(Locale.ROOT);
	}

}
