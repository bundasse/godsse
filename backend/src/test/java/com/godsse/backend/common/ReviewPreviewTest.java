package com.godsse.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 카드 미리보기 만들기 — 스포일러가 새어 나가지 않는지 확인한다.
 *
 * <p>
 * 순수한 문자열 처리라서 Spring 없이 빠르게 돌릴 수 있다.
 */
class ReviewPreviewTest {

	@Test
	@DisplayName("본문이 짧으면 그대로 돌려준다")
	void shortBody() {
		assertThat(ReviewPreview.of("짧은 감상입니다.")).isEqualTo("짧은 감상입니다.");
	}

	@Test
	@DisplayName("길면 정해진 길이에서 자르고 말줄임표를 붙인다")
	void longBody() {
		String preview = ReviewPreview.of("가".repeat(300), 200);

		assertThat(preview).hasSize(201);
		assertThat(preview).endsWith("…");
	}

	@Test
	@DisplayName("접기 표시가 앞부분에 있으면 그 앞까지만 보여 준다")
	void stopsAtFoldMarker() {
		String body = "가".repeat(50) + "\n" + SpoilerMarkers.FOLD + "\n결말은 이렇습니다 " + "나".repeat(300);

		String preview = ReviewPreview.of(body, 200);

		assertThat(preview).doesNotContain(SpoilerMarkers.FOLD);
		assertThat(preview).doesNotContain("결말은 이렇습니다");
	}

	@Test
	@DisplayName("가리기 표시의 짝이 잘려 나갔으면 그 지점부터 버린다")
	void cutsUnmatchedBlurMarker() {
		String body = "가".repeat(50) + SpoilerMarkers.BLUR + "범인은 집사입니다" + "나".repeat(300);

		String preview = ReviewPreview.of(body, 200);

		assertThat(preview).doesNotContain(SpoilerMarkers.BLUR);
		assertThat(preview).doesNotContain("범인은 집사");
	}

	@Test
	@DisplayName("짝이 맞는 가리기 표시는 그대로 남긴다 (화면에서 가려 준다)")
	void keepsMatchedBlurMarker() {
		String body = "가".repeat(50) + SpoilerMarkers.BLUR + "짧은 스포일러" + SpoilerMarkers.BLUR + "나".repeat(300);

		String preview = ReviewPreview.of(body, 200);

		assertThat(preview).contains(SpoilerMarkers.BLUR + "짧은 스포일러" + SpoilerMarkers.BLUR);
	}

}
