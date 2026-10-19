package com.godsse.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.godsse.backend.domain.ReviewLike;

/**
 * 좋아요(하트) 조회/저장.
 */
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

	boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

	Optional<ReviewLike> findByReviewIdAndUserId(Long reviewId, Long userId);

	/** 이 감상의 좋아요 수. */
	long countByReviewId(Long reviewId);

	/**
	 * 목록에서 "내가 좋아요한 감상 id" 만 한 번에 가져온다.
	 *
	 * <p>
	 * 카드마다 좋아요 여부를 묻는 대신(N+1), 페이지에 담긴 id 들을 넘겨 1회로 끝낸다.
	 */
	@Query("select l.review.id from ReviewLike l where l.user.id = :userId and l.review.id in :reviewIds")
	List<Long> findLikedReviewIds(@Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);

}
