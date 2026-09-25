package com.godsse.backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.domain.User;
import com.godsse.backend.repository.UserRepository;

/**
 * 사용자 조회·수정 로직.
 *
 * <p>
 * 클래스에 붙은 {@code @Transactional(readOnly = true)} 는 "이 클래스의 기본은 읽기 전용"이라는 뜻이다.
 * 값을 바꾸는 메서드에만 {@code @Transactional} 을 따로 붙여 쓰기 트랜잭션으로 바꾼다.
 * (읽기 전용은 불필요한 변경 감지를 하지 않아 조금 더 빠르다)
 */
@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public Optional<User> findById(Long id) {
		return this.userRepository.findById(id);
	}

	public User getByHandle(String handle) {
		return this.userRepository.findByHandle(handle)
			.orElseThrow(() -> new NotFoundException("handle", "해당 핸들의 사용자를 찾을 수 없습니다."));
	}

	public boolean existsByHandle(String handle) {
		return this.userRepository.existsByHandle(handle);
	}

}
