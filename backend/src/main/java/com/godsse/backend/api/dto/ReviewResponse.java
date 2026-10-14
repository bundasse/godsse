package com.godsse.backend.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import com.godsse.backend.domain.Review;
import com.godsse.backend.domain.enums.SpoilerMode;
import com.godsse.backend.domain.enums.Visibility;

/**
 * 감상 전문(상세 화면) 응답.
 *
 * <p>
 * 목록용 {@link ReviewSummary} 와 달리 본문 전체와 공개 범위가 담긴다.
 *
 * @param id           감상 번호
 * @param author       쓴 사람
 * @param recipient    받는 사람
 * @param playedOn     플레이 날짜
 * @param ruleName     룰
 * @param scenarioName 시나리오명(없으면 null)
 * @param body         본문 전체(스포일러 마커 포함)
 * @param spoilerMode  스포일러 표시 방식
 * @param visibility   공개 범위
 * @param tags         감정 태그
 * @param createdAt    작성 시각
 * @param updatedAt    수정 시각
 * @param editable     조회자가 이 감상을 수정·삭제할 수 있는지(작성자 본인 여부)
 */
public record ReviewResponse(Long id, UserSummary author, UserSummary recipient, LocalDate playedOn, String ruleName,
		String scenarioName, String body, SpoilerMode spoilerMode, Visibility visibility, Set<String> tags,
		Instant createdAt, Instant updatedAt, boolean editable) {

	public static ReviewResponse of(Review review, boolean editable) {
		return new ReviewResponse(review.getId(), UserSummary.of(review.getAuthor()),
				UserSummary.of(review.getRecipient()), review.getPlayedOn(), review.getRuleName(),
				review.getScenarioName(), review.getBody(), review.getSpoilerMode(), review.getVisibility(),
				review.getTags(), review.getCreatedAt(), review.getUpdatedAt(), editable);
	}

}
