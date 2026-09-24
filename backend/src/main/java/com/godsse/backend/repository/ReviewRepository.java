package com.godsse.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.Review;

/**
 * 리뷰(감상 메시지) 조회/저장.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

	/** 특정 사용자가 받은 리뷰 목록(마이페이지 카드). */
	Page<Review> findByRecipientId(Long recipientId, Pageable pageable);

	/** 특정 사용자가 쓴 리뷰 목록. */
	Page<Review> findByAuthorId(Long authorId, Pageable pageable);

	/** 홈 피드용 전체 목록(플레이 날짜 최신순, 같으면 작성 최신순). */
	Page<Review> findAllByOrderByPlayedOnDescCreatedAtDesc(Pageable pageable);

	/** 받은 리뷰 개수. */
	long countByRecipientId(Long recipientId);

}
