package com.godsse.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화 도구를 빈으로 등록한다.
 *
 * <p>
 * BCrypt 는 비밀번호를 되돌릴 수 없는 형태(해시)로 바꾸는 알고리즘이다. 같은 비밀번호라도
 * 매번 다른 결과가 나오므로, 저장된 값만 보고 원문을 알아낼 수 없다. 로그인할 때는
 * {@code passwordEncoder.matches(입력값, 저장된해시)} 로 비교한다.
 *
 * <p>
 * 재사용 참고: Spring Security 전체를 도입하지 않고 암호화 모듈만 사용한다.
 */
@Configuration
public class PasswordConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
