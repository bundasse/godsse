package com.godsse.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 이미 존재하는 값과 충돌할 때(409). 예: 중복된 핸들·이메일, 중복 팔로우.
 *
 * <p>
 * {@code field} 를 함께 넘기면 화면에서 해당 입력칸 옆에 오류를 표시할 수 있다.
 */
public class ConflictException extends BusinessException {

	public ConflictException(String message) {
		super(HttpStatus.CONFLICT, null, message);
	}

	public ConflictException(String field, String message) {
		super(HttpStatus.CONFLICT, field, message);
	}

}
