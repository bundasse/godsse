package com.godsse.backend.common;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.godsse.backend.common.exception.UnauthorizedException;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 컨트롤러 파라미터의 {@link LoginUser} 를 실제 로그인 사용자로 채워 주는 변환기.
 *
 * <p>
 * 처리 순서:
 * <ol>
 * <li>세션에서 {@code LOGIN_USER_ID} 를 꺼낸다. 없으면 로그인하지 않은 상태다.</li>
 * <li>{@code required = true} 인데 로그인하지 않았다면 401 을 던진다.</li>
 * <li>id 로 DB 에서 사용자를 읽는다. 세션에는 id 만 있고 최신 정보는 DB 가 기준이다.</li>
 * <li>사용자가 사라졌다면(탈퇴 등) 세션을 지우고 다시 로그인하도록 안내한다.</li>
 * </ol>
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

	private final UserService userService;

	public LoginUserArgumentResolver(UserService userService) {
		this.userService = userService;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(LoginUser.class)
				&& User.class.isAssignableFrom(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

		LoginUser annotation = parameter.getParameterAnnotation(LoginUser.class);
		boolean required = annotation == null || annotation.required();

		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		HttpSession session = request == null ? null : request.getSession(false);
		Long userId = session == null ? null : (Long) session.getAttribute(SessionKeys.LOGIN_USER_ID);

		if (userId == null) {
			if (required) {
				throw new UnauthorizedException("로그인이 필요합니다.");
			}
			return null;
		}

		Optional<User> found = this.userService.findById(userId);
		if (found.isEmpty()) {
			// 세션은 남아 있는데 사용자가 없는 경우이므로 로그인 정보를 지운다.
			session.invalidate();
			if (required) {
				throw new UnauthorizedException("로그인 정보가 만료되었습니다. 다시 로그인해 주세요.");
			}
			return null;
		}

		return found.get();
	}

}
