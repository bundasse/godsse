package com.godsse.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.User;

/**
 * 사용자 조회/저장.
 *
 * <p>
 * {@code JpaRepository} 를 상속하면 저장(save)·조회(findById)·삭제(delete) 같은 기본 기능이 자동으로 생긴다.
 * 메서드 이름 규칙({@code findBy...})만 맞춰 선언하면 Spring Data JPA 가 구현 코드를 만들어 준다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByHandle(String handle);

	boolean existsByHandle(String handle);

	boolean existsByEmail(String email);

	/** 핸들 또는 닉네임에 검색어가 포함된 사용자 목록(대소문자 무시). */
	Page<User> findByHandleContainingIgnoreCaseOrNicknameContainingIgnoreCase(String handle, String nickname,
			Pageable pageable);

}
