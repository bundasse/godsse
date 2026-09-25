package com.godsse.backend.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.LoginRequest;
import com.godsse.backend.api.dto.SignupRequest;
import com.godsse.backend.api.dto.UserProfileResponse;
import com.godsse.backend.common.exception.ConflictException;
import com.godsse.backend.common.exception.UnauthorizedException;
import com.godsse.backend.domain.User;
import com.godsse.backend.repository.UserRepository;

/**
 * 회원가입과 로그인 처리.
 *
 * <p>
 * 비밀번호는 절대 그대로 저장하지 않는다. {@link PasswordEncoder}(BCrypt)로 바꾼 값만 저장하고,
 * 로그인할 때는 {@code matches(입력값, 저장된값)} 로 비교한다.
 *
 * <p>
 * 응답은 엔티티가 아니라 DTO({@link UserProfileResponse})로 돌려준다. 트랜잭션 밖으로 엔티티를
 * 내보내면 지연 로딩 문제가 생길 수 있기 때문이다. (doc/study/01-jpa-and-entity.md 참고)
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * 회원가입 — 핸들·이메일 중복을 확인하고 비밀번호를 해시해서 저장한다.
	 *
	 * @throws ConflictException 핸들 또는 이메일이 이미 사용 중일 때
	 */
	@Transactional
	public UserProfileResponse signup(SignupRequest request) {
		String handle = normalizeHandle(request.handle());
		String email = normalizeEmail(request.email());

		if (this.userRepository.existsByHandle(handle)) {
			throw new ConflictException("handle", "이미 사용 중인 핸들입니다.");
		}
		if (this.userRepository.existsByEmail(email)) {
			throw new ConflictException("email", "이미 가입된 이메일입니다.");
		}

		String passwordHash = this.passwordEncoder.encode(request.password());
		User saved = this.userRepository.save(new User(handle, email, passwordHash, request.nickname().trim()));

		return UserProfileResponse.of(saved);
	}

	/**
	 * 로그인 — 핸들과 비밀번호를 확인한다.
	 *
	 * <p>
	 * 핸들이 없는 경우와 비밀번호가 틀린 경우에 <b>같은 메시지</b>를 돌려준다.
	 * 어떤 핸들이 가입되어 있는지 알려 주지 않기 위함이다.
	 *
	 * @throws UnauthorizedException 핸들 또는 비밀번호가 올바르지 않을 때
	 */
	@Transactional(readOnly = true)
	public UserProfileResponse login(LoginRequest request) {
		User user = this.userRepository.findByHandle(normalizeHandle(request.handle()))
			.orElseThrow(() -> new UnauthorizedException("핸들 또는 비밀번호가 올바르지 않습니다."));

		if (!this.passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException("핸들 또는 비밀번호가 올바르지 않습니다.");
		}

		return UserProfileResponse.of(user);
	}

	/** 핸들은 저장·조회 모두 소문자로 통일한다. (화면에서는 대소문자 구분 없이 쓸 수 있다) */
	private static String normalizeHandle(String handle) {
		return handle.trim().toLowerCase(Locale.ROOT);
	}

	/** 이메일도 앞뒤 공백 제거 + 소문자로 통일한다. */
	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
