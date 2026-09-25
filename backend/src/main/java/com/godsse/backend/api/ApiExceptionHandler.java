package com.godsse.backend.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.godsse.backend.api.dto.FieldValidationError;
import com.godsse.backend.api.dto.ValidationErrorResponse;
import com.godsse.backend.common.exception.BusinessException;

/**
 * API 예외를 일관된 JSON 형태로 변환한다.
 *
 * <p>
 * 실패 응답의 형태는 아래 하나로 통일된다. (프론트의 {@code client.js} 가 이 형태를 읽어 Error 로 바꾼다)
 *
 * <pre>
 * { "message": "...", "errors": [ { "field": "handle", "reason": "..." } ] }
 * </pre>
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

	/**
	 * 서비스에서 의도적으로 던진 예외({@link BusinessException})를 그 예외가 들고 있는 상태 코드로 변환한다.
	 *
	 * <p>
	 * 예: 중복 핸들 → 409, 없는 사용자 → 404, 로그인 필요 → 401.
	 * {@code field} 가 지정되어 있으면 {@code errors[]} 에 담아 주므로 화면에서 해당 입력칸 옆에 표시할 수 있다.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ValidationErrorResponse> handleBusiness(BusinessException exception) {
		List<FieldValidationError> errors = exception.getField() == null ? List.of()
				: List.of(new FieldValidationError(exception.getField(), exception.getMessage()));

		return ResponseEntity.status(exception.getStatus())
			.body(new ValidationErrorResponse(exception.getMessage(), errors));
	}

	/**
	 * 요청 본문(JSON)을 읽을 수 없을 때 → 400.
	 *
	 * <p>
	 * 이 처리가 없으면 Spring 의 기본 오류 응답이 내려가는데, 그 응답에는 내부 스택 트레이스가 담긴다.
	 * 클라이언트가 형식을 잘못 보낸 경우이므로 우리 규격의 400 으로 바꿔 준다.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ValidationErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(new ValidationErrorResponse("요청 본문을 읽을 수 없습니다. JSON 형식을 확인해 주세요.", List.of()));
	}

}
