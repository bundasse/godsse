package com.godsse.backend.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.godsse.backend.api.dto.FieldValidationError;
import com.godsse.backend.api.dto.ValidationErrorResponse;

/**
 * API 예외를 일관된 JSON 형태로 변환한다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	/**
	 * {@code @Valid} 검증 실패 시 400 응답으로 변환한다.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		List<FieldValidationError> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
			.toList();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ValidationErrorResponse("요청 값이 올바르지 않습니다.", errors));
	}

}
