package com.godsse.backend.common;

/**
 * 카드(목록)에 보여 줄 본문 미리보기를 만든다.
 *
 * <p>
 * 목록에서는 본문 전체를 내려주지 않는다. 긴 글을 20건씩 내려보내면 응답이 커지고,
 * 어차피 화면에서 3줄만 보여 주기 때문이다.
 *
 * <p>
 * <b>주의</b>: 그냥 앞에서 잘라 내면 스포일러가 새어 나갈 수 있다.
 * <ul>
 * <li>접기 표시({@code :::spoiler}) 뒤쪽은 잘라 낸다.</li>
 * <li>가리기 표시({@code ||})의 짝이 잘려서 안 맞으면 그 지점부터 버린다.
 * (짝이 안 맞는 마커는 화면에서 "그대로 노출"로 처리하므로 위험하다)</li>
 * </ul>
 */
public final class ReviewPreview {

	/** 카드에 담을 미리보기 길이(글자 수). */
	public static final int CARD_LENGTH = 200;

	private static final String ELLIPSIS = "…";

	public static String of(String body) {
		return of(body, CARD_LENGTH);
	}

	public static String of(String body, int limit) {
		if (body == null) {
			return "";
		}

		String text = body.strip();
		if (text.length() <= limit) {
			return text;
		}

		int end = limit;

		int foldIndex = text.indexOf(SpoilerMarkers.FOLD);
		if (foldIndex >= 0 && foldIndex < end) {
			end = foldIndex;
		}

		String cut = text.substring(0, end);

		if (count(cut, SpoilerMarkers.BLUR) % 2 != 0) {
			cut = cut.substring(0, cut.lastIndexOf(SpoilerMarkers.BLUR));
		}

		return cut.stripTrailing() + ELLIPSIS;
	}

	private static int count(String text, String marker) {
		int total = 0;
		int index = text.indexOf(marker);
		while (index >= 0) {
			total++;
			index = text.indexOf(marker, index + marker.length());
		}
		return total;
	}

	private ReviewPreview() {
	}

}
