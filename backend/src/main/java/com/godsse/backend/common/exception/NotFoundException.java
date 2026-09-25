package com.godsse.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 찾는 대상이 없을 때(404). 예: 존재하지 않는 핸들의 프로필 조회.
 */
public class NotFoundException extends BusinessException {

	public NotFoundException(String message) {
		super(HttpStatus.NOT_FOUND, null, message);
	}

	public NotFoundException(String field, String message) {
		super(HttpStatus.NOT_FOUND, field, message);
	}

}
