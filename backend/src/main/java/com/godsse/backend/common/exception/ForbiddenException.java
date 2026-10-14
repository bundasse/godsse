package com.godsse.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 로그인은 했지만 그 자원을 다룰 권한이 없을 때(403).
 *
 * <p>
 * 예: 남이 쓴 감상을 수정·삭제하려 할 때, 공개 범위 밖의 감상을 보려 할 때.
 */
public class ForbiddenException extends BusinessException {

	public ForbiddenException(String message) {
		super(HttpStatus.FORBIDDEN, null, message);
	}

}
