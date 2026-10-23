package com.godsse.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.common.Handles;
import com.godsse.backend.domain.User;

/**
 * 사용자 조회/저장.
 *
 * <p>
 * {@code JpaRepository} 를 상속하면 저장(save)·조회(findById)·삭제(delete) 같은 기본 기능이 자동으로 생긴다.
 * 메서드 이름 규칙({@code findBy...})만 맞춰 선언하면 Spring Data JPA 가 구현 코드를 만들어 준다.
 *
 * <p>
 * <b>핸들 조회는 {@link #findByNormalizedHandle(String)} 을 쓴다.</b>
 * 이름 그대로 정규화(공백 제거·소문자)까지 해 주므로, 대소문자가 섞여 들어와도 같은 사람을 찾는다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByHandle(String handle);

	/**
	 * 핸들로 사용자를 찾는다. (항상 {@link Handles#normalize(String)} 후 조회)
	 *
	 * <p>
	 * {@code default} 메서드라 Spring Data 가 쿼리로 만들지 않고, 위의 {@code findByHandle} 을 그대로 쓴다.
	 */
	default Optional<User> findByNormalizedHandle(String handle) {
		return findByHandle(Handles.normalize(handle));
	}

	boolean existsByHandle(String handle);

	boolean existsByEmail(String email);

	/** 핸들 또는 닉네임에 검색어가 포함된 사용자 목록(대소문자 무시). */
	Page<User> findByHandleContainingIgnoreCaseOrNicknameContainingIgnoreCase(String handle, String nickname,
			Pageable pageable);

}
