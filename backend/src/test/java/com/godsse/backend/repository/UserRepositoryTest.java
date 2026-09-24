package com.godsse.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.domain.User;

/**
 * 엔티티 매핑과 리포지토리가 정상 동작하는지 확인한다.
 *
 * <p>
 * {@code @Transactional} 이 붙으면 테스트가 끝날 때 변경 내용이 롤백되므로 DB 가 더러워지지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("User 를 저장하면 handle 로 조회할 수 있고 생성/수정 시각이 채워진다")
	void saveAndFindByHandle() {
		User saved = this.userRepository.save(new User("godsse", "godsse@example.com", "hash", "고드세"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(this.userRepository.findByHandle("godsse")).isPresent();
	}

	@Test
	@DisplayName("existsByHandle 은 핸들 중복 여부를 판별한다")
	void existsByHandle() {
		this.userRepository.save(new User("godsse", "godsse@example.com", "hash", "고드세"));

		assertThat(this.userRepository.existsByHandle("godsse")).isTrue();
		assertThat(this.userRepository.existsByHandle("unknown")).isFalse();
	}

}
