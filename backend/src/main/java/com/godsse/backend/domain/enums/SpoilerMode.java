package com.godsse.backend.domain.enums;

/**
 * 리뷰에 스포일러가 포함되어 있을 때, 카드·상세 화면에서 본문을 어떻게 가릴지 나타낸다.
 *
 * <ul>
 * <li>{@link #NONE} — 스포일러 없음. 본문을 그대로 보여 준다.</li>
 * <li>{@link #BLUR} — 스포일러 포함. 본문을 흐리게 가리고 클릭하면 보여 준다.</li>
 * <li>{@link #FOLD} — 스포일러 포함. 본문을 접어 두고 "펼치기"를 눌러야 보여 준다.</li>
 * </ul>
 *
 * <p>
 * 본문 안의 일부 문장만 가리는 것은 마커({@code ||문장||})로 처리하며, 이 값은 리뷰 전체에 대한 설정이다.
 */
public enum SpoilerMode {
	NONE,
	BLUR,
	FOLD
}
