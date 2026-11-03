package com.godsse.backend.common;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 룰 색상의 기본 팔레트와 형식 검증.
 *
 * <p>
 * 사용자가 색을 지정하지 않은 룰도 히트맵에서 서로 구분되어야 하므로 <b>이름에서 색을 계산</b>한다.
 * 같은 룰 이름이면 서버를 다시 켜도 항상 같은 색이 나오도록 {@link String#hashCode()} 를 쓴다.
 * (자바 명세가 해시 값을 고정하고 있어 실행 환경이 달라도 결과가 같다)
 *
 * <p>
 * 색은 저장 전에 소문자로 맞춘다. 그래야 {@code #FF8800} 과 {@code #ff8800} 이
 * 같은 색으로 비교되고, 화면에서도 색 견본과 문자열이 어긋나지 않는다.
 */
public final class RuleColors {

	/**
	 * 기본 팔레트. 서로 구분되기 쉬운 10색이며 모두 {@code #RRGGBB} 형식이다.
	 */
	private static final List<String> PALETTE = List.of("#2563eb", "#16a34a", "#dc2626", "#d97706", "#7c3aed",
			"#0891b2", "#db2777", "#65a30d", "#ea580c", "#4f46e5");

	/** 사용자가 보낸 색의 형식({@code #RRGGBB}). */
	private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

	private RuleColors() {
	}

	/**
	 * 룰 이름에 어울리는 기본 색을 고른다(항상 팔레트 안의 값).
	 *
	 * @param ruleName 룰 이름(자유 입력 문자열). null 이어도 예외 없이 색을 돌려준다
	 */
	public static String defaultColor(String ruleName) {
		int hash = ruleName == null ? 0 : ruleName.hashCode();
		return PALETTE.get(Math.floorMod(hash, PALETTE.size()));
	}

	/** {@code #RRGGBB} 형식인지 확인한다. */
	public static boolean isValid(String color) {
		return color != null && HEX_COLOR.matcher(color).matches();
	}

	/** 기본 팔레트 사본(테스트·문서용). */
	public static List<String> palette() {
		return PALETTE;
	}

}
