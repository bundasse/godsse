package com.godsse.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.godsse.backend.api.dto.LoginRequest;
import com.godsse.backend.api.dto.SignupRequest;
import com.godsse.backend.api.dto.UserProfileResponse;
import com.godsse.backend.common.LoginUser;
import com.godsse.backend.common.SessionKeys;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * 회원가입 / 로그인 / 로그아웃 / 내 정보.
 *
 * <p>
 * 로그인 상태는 서버의 {@link HttpSession} 에 저장되고, 브라우저에는 세션 ID 쿠키(JSESSIONID)만 남는다.
 * 그래서 프론트엔드는 토큰을 따로 보관할 필요가 없고, 자바스크립트로 그 쿠키를 읽을 수도 없다(HttpOnly).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * 회원가입. 가입 직후 바로 로그인 상태가 된다(201 Created).
	 */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public UserProfileResponse signup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
		UserProfileResponse profile = this.authService.signup(request);
		startSession(httpRequest, profile.id());
		return profile;
	}

	/**
	 * 로그인. 성공하면 세션을 새로 시작하고 프로필을 돌려준다.
	 */
	@PostMapping("/login")
	public UserProfileResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		UserProfileResponse profile = this.authService.login(request);
		startSession(httpRequest, profile.id());
		return profile;
	}

	/**
	 * 로그아웃. 세션을 통째로 버린다(204 No Content).
	 */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest httpRequest) {
		HttpSession session = httpRequest.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

	/**
	 * 지금 로그인한 사용자의 정보. 로그인하지 않았다면 401 이다.
	 */
	@GetMapping("/me")
	public UserProfileResponse me(@LoginUser User user) {
		return UserProfileResponse.of(user);
	}

	/**
	 * 로그인 성공 시 세션을 새로 시작한다.
	 *
	 * <p>
	 * 로그인 전에 쓰던 세션 ID 를 그대로 두면 "세션 고정(session fixation)" 공격에 노출되므로
	 * {@code changeSessionId()} 로 ID 를 새로 발급받은 뒤 사용자 번호를 담는다.
	 */
	private void startSession(HttpServletRequest request, Long userId) {
		request.getSession(true);
		request.changeSessionId();
		request.getSession().setAttribute(SessionKeys.LOGIN_USER_ID, userId);
	}

}
