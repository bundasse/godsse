package com.godsse.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청 형식은 맞지만 값이 규칙에 어긋날 때(400).
 *
 * <p>
 * 예: 업로드한 파일이 이미지가 아니거나 허용 크기를 넘을 때.
 * ({@code @Valid} 로 표현할 수 없는 검증은 서비스에서 이 예외를 던진다)
 */
public class BadRequestException extends BusinessException {

	public BadRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, null, message);
	}

	public BadRequestException(String field, String message) {
		super(HttpStatus.BAD_REQUEST, field, message);
	}

}
