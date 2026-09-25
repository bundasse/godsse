package com.godsse.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 로그인이 필요하거나 인증 정보가 틀렸을 때(401).
 *
 * <p>
 * 예: 로그인하지 않고 마이페이지 API 호출, 비밀번호 불일치.
 */
public class UnauthorizedException extends BusinessException {

	public UnauthorizedException(String message) {
		super(HttpStatus.UNAUTHORIZED, null, message);
	}

}
