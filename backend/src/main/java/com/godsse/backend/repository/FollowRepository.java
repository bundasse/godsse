package com.godsse.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.Follow;

/**
 * 팔로우 관계 조회/저장.
 *
 * <p>
 * {@code findByFollowerId...} 처럼 엔티티의 연관 객체({@code follower}) 뒤에 {@code Id} 를 붙이면
 * 그 연관 객체의 기본키를 조건으로 삼는다는 뜻이다.
 */
public interface FollowRepository extends JpaRepository<Follow, Long> {

	boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

	Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

	/** 나를 팔로우하는 사람 수(팔로워 수). */
	long countByFollowingId(Long followingId);

	/** 내가 팔로우하는 사람 수(팔로잉 수). */
	long countByFollowerId(Long followerId);

	/** 나를 팔로우하는 사람 목록. */
	Page<Follow> findByFollowingId(Long followingId, Pageable pageable);

	/** 내가 팔로우하는 사람 목록. */
	Page<Follow> findByFollowerId(Long followerId, Pageable pageable);

}
