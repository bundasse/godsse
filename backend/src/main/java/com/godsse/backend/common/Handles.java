package com.godsse.backend.common;

import java.util.Locale;

/**
 * 핸들(사용자 아이디) 정규화 규칙.
 *
 * <p>
 * 핸들은 <b>저장할 때도, 찾을 때도 소문자</b>로 통일한다. 그래야 {@code /u/Godsse} 로 들어와도
 * {@code godsse} 를 찾는다. 규칙을 한 곳에 모아 두면 "가입은 소문자로 저장하는데 조회는 그대로 비교해서
 * 404 가 나는" 어긋남이 생기지 않는다.
 *
 * <p>
 * 실제 조회는 {@code UserRepository.findByNormalizedHandle(...)} 를 쓰면 되고,
 * 이 클래스는 <b>저장 전 정규화</b>(회원가입)와 <b>중복 확인</b>에서 직접 쓴다.
 */
public final class Handles {

	private Handles() {
	}

	/**
	 * 앞뒤 공백을 지우고 소문자로 바꾼다.
	 *
	 * <p>
	 * {@code null} 은 빈 문자열로 바꾼다. (DB 에 빈 핸들이 저장될 일은 없지만, 조회에서 NPE 를 막는다)
	 */
	public static String normalize(String handle) {
		if (handle == null) {
			return "";
		}

		return handle.trim().toLowerCase(Locale.ROOT);
	}

}
