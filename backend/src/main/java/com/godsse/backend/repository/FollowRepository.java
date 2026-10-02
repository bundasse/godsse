package com.godsse.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.godsse.backend.domain.Follow;
import com.godsse.backend.domain.User;

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

	/**
	 * 나를 팔로우하는 "사용자" 목록.
	 *
	 * <p>
	 * {@code Follow} 행을 가져와 {@code getFollower()} 로 읽으면 사람 수만큼 SELECT 가 더 나간다(N+1).
	 * JPQL 로 "처음부터 User 를 고르면" 한 번의 쿼리로 끝난다.
	 *
	 * <p>
	 * 정렬은 쿼리 안에 적는다. {@code Pageable} 의 Sort 를 쓰면 JPQL 별칭을 해석하지 못해 오류가 난다.
	 */
	@Query("select f.follower from Follow f where f.following.id = :userId order by f.createdAt desc")
	Page<User> findFollowerUsers(@Param("userId") Long userId, Pageable pageable);

	/** 내가 팔로우하는 "사용자" 목록. */
	@Query("select f.following from Follow f where f.follower.id = :userId order by f.createdAt desc")
	Page<User> findFollowingUsers(@Param("userId") Long userId, Pageable pageable);

}
