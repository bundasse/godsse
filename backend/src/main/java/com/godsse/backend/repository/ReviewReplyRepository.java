package com.godsse.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.godsse.backend.domain.ReviewReply;

/**
 * 답장 조회/저장.
 */
public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

	/** 한 감상의 답장 목록(오래된 순 = 대화 순서). */
	List<ReviewReply> findByReviewIdOrderByCreatedAtAsc(Long reviewId);

	/** 이 감상의 답장 수. */
	long countByReviewId(Long reviewId);

}
