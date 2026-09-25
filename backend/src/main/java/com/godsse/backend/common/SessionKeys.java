package com.godsse.backend.common;

/**
 * HTTP 세션(로그인 상태를 서버가 기억하는 저장소)에 담는 값의 키 모음.
 *
 * <p>
 * 세션에는 사용자 객체 전체가 아니라 <b>사용자 번호(id)</b> 만 담는다.
 * 최신 정보(닉네임 변경 등)는 항상 DB 에서 다시 읽어 오기 위함이다.
 */
public final class SessionKeys {

	/** 로그인한 사용자의 id (Long). */
	public static final String LOGIN_USER_ID = "LOGIN_USER_ID";

	private SessionKeys() {
	}

}
