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

	/**
	 * 이 사람이 쓴 감상들에 달린 답장 수의 합.
	 *
	 * <p>
	 * 이름에 밑줄이 없는 {@code countByReviewAuthorId} 는 {@code review.author.id} 로 읽힌다.
	 * (엔티티의 필드 이름을 따라가며 해석하는 "파생 쿼리")
	 */
	long countByReviewAuthorId(Long authorId);

}
