package com.godsse.backend.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드의 파라미터로 "지금 로그인한 사용자"를 받고 싶을 때 붙인다.
 *
 * <pre>
 * public UserProfileResponse me({@code @LoginUser} User user) { ... }
 * public UserProfileResponse profile({@code @LoginUser(required = false)} User me, ...) { ... }
 * </pre>
 *
 * <p>
 * 실제 값은 {@link LoginUserArgumentResolver} 가 세션에서 꺼내 DB 에서 읽어 채워 준다.
 * Vue 의 전역 스토어(예: 로그인 사용자 상태)를 서버 쪽에서 자동으로 채워 주는 것과 비슷하다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginUser {

	/** true(기본)이면 로그인하지 않았을 때 401 을 응답한다. */
	boolean required() default true;

}
