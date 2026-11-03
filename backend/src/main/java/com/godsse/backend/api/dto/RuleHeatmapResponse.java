package com.godsse.backend.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 마이페이지의 연간 룰 히트맵 — GitHub 잔디 그래프와 같은 모양.
 *
 * <p>
 * 화면은 달력 칸(7행 × 주 단위 열)을 그대로 만들고, 서버는 <b>일정이 있는 날만</b> 내려준다.
 * 1년치라도 하루 한 칸이면 366칸이므로, 빈 날까지 채워 보내면 응답만 커지고 얻는 것이 없다.
 *
 * @param year       조회한 연도
 * @param totalCount 그 해 일정 수
 * @param days       일정이 있는 날(날짜 오름차순)
 * @param rules      그 해에 쓴 룰(많은 순 → 같으면 이름 순) — 범례·색 편집에 쓴다
 */
public record RuleHeatmapResponse(int year, int totalCount, List<Day> days, List<Rule> rules) {

	/**
	 * 하루 칸.
	 *
	 * @param date      날짜
	 * @param count     그 날 잡은 세션 수(같은 룰로 2회면 2)
	 * @param ruleNames 그 날 쓴 룰 이름들(중복 없이, 일정을 만든 순서)
	 */
	public record Day(LocalDate date, int count, List<String> ruleNames) {
	}

	/**
	 * 범례 한 줄(그 해에 쓴 룰 하나).
	 *
	 * @param ruleName 룰 이름(자유 입력 문자열)
	 * @param color    칸에 칠할 색({@code #rrggbb}). 사용자 지정색이 있으면 그 색
	 * @param count    그 해 그 룰로 잡은 세션 수
	 * @param custom   사용자가 색을 직접 고른 상태인지(아니면 기본 팔레트 색)
	 */
	public record Rule(String ruleName, String color, int count, boolean custom) {
	}

}
