package com.godsse.backend.common;

/**
 * 감상 본문에 쓰는 스포일러 마커.
 *
 * <p>
 * 백엔드는 마커를 해석하지 않고 <b>문자 그대로 저장</b>한다. 화면에서 마커를 찾아 가리거나 접는다.
 * (마커를 문자로 남겨야 작성자가 글을 고칠 때 위치를 다시 계산하지 않아도 된다)
 *
 * <p>
 * 프론트엔드의 {@code src/constants/spoilerMarkers.js} 와 반드시 같은 값이어야 한다.
 *
 * <ul>
 * <li>{@link #BLUR} — {@code ||문장||} 처럼 문장을 감싸면 그 부분만 가린다.</li>
 * <li>{@link #FOLD} — 단독 줄로 쓰면 그 지점 이후를 접는다.</li>
 * </ul>
 */
public final class SpoilerMarkers {

	public static final String BLUR = "||";

	public static final String FOLD = ":::spoiler";

	private SpoilerMarkers() {
	}

}
