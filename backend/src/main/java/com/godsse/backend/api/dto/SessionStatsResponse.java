package com.godsse.backend.api.dto;

import java.util.List;

/**
 * 마이페이지의 연간 세션 통계.
 *
 * @param year          조회한 연도
 * @param totalCount    그 해 일정 수
 * @param plannedCount  예정
 * @param doneCount     완료
 * @param canceledCount 취소
 * @param gmCount       내가 GM 이었던 횟수
 * @param monthlyCounts 1월~12월 횟수(항상 12개)
 * @param rules         룰별 횟수(많은 순)
 */
public record SessionStatsResponse(int year, int totalCount, int plannedCount, int doneCount, int canceledCount,
		int gmCount, List<Integer> monthlyCounts, List<RuleCount> rules) {

	/**
	 * 룰별 사용 횟수.
	 *
	 * @param ruleName 룰 이름(자유 입력 문자열)
	 * @param count    그 룰로 잡은 세션 수
	 */
	public record RuleCount(String ruleName, int count) {
	}

}
