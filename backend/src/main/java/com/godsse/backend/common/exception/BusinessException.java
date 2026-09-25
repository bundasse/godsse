package com.godsse.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 서비스 계층에서 "의도적으로" 발생시키는 예외의 상위 클래스.
 *
 * <p>
 * 예외마다 상태 코드(404/409/401 등)와 메시지를 함께 들고 다니고,
 * {@code ApiExceptionHandler} 가 이를 잡아 {@code { message, errors[] }} JSON 으로 바꾼다.
 *
 * <p>
 * Vue 비유: 프론트에서 {@code throw new Error(메시지)} 를 던지고 호출부에서 잡는 것과 같다.
 * 다만 HTTP 상태 코드를 함께 정할 수 있도록 {@link HttpStatus} 를 들고 있는다.
 */
public class BusinessException extends RuntimeException {

	private final HttpStatus status;

	/** 응답의 {@code errors[].field} 에 넣을 값. 특정 입력값과 무관하면 null. */
	private final String field;

	public BusinessException(HttpStatus status, String field, String message) {
		super(message);
		this.status = status;
		this.field = field;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public String getField() {
		return this.field;
	}

}
