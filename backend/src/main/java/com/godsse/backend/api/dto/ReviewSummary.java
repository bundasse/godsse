package com.godsse.backend.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.godsse.backend.common.ReviewPreview;
import com.godsse.backend.domain.Review;
import com.godsse.backend.domain.enums.SpoilerMode;

/**
 * 목록(카드)에 쓰는 감상 요약.
 *
 * <p>
 * 본문은 전체가 아니라 {@link ReviewPreview} 로 만든 미리보기만 담는다.
 * (화면에서는 어차피 3줄만 보여 주고, 잘리는 지점에서 스포일러가 새어 나가지 않도록 안전하게 자른 값)
 *
 * @param id           감상 번호
 * @param author       쓴 사람
 * @param recipient    받는 사람
 * @param playedOn     플레이 날짜
 * @param ruleName     룰
 * @param scenarioName 시나리오명(없으면 null)
 * @param bodyPreview  본문 미리보기
 * @param spoilerMode  스포일러 표시 방식
 * @param tags         감정 태그
 * @param createdAt    작성 시각
 */
public record ReviewSummary(Long id, UserSummary author, UserSummary recipient, LocalDate playedOn, String ruleName,
		String scenarioName, String bodyPreview, SpoilerMode spoilerMode, Set<String> tags, Instant createdAt) {

	public static ReviewSummary of(Review review) {
		return new ReviewSummary(review.getId(), UserSummary.of(review.getAuthor()),
				UserSummary.of(review.getRecipient()), review.getPlayedOn(), review.getRuleName(),
				review.getScenarioName(), ReviewPreview.of(review.getBody()), review.getSpoilerMode(),
				review.getTags(), review.getCreatedAt());
	}

}
