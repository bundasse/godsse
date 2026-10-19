package com.godsse.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 감상에 대한 좋아요(하트) 한 건. "누가(user) 어느 감상(review)을 좋아요했는가".
 *
 * <p>
 * 같은 사람이 같은 감상에 두 번 좋아요할 수 없도록 {@code (review_id, user_id)} 에 유니크 제약을 둔다.
 * (서비스에서도 확인해 409 를 주지만, DB 제약이 마지막 안전장치다)
 */
@Entity
@Table(name = "review_likes",
		uniqueConstraints = @UniqueConstraint(name = "uk_review_likes", columnNames = { "review_id", "user_id" }))
public class ReviewLike extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	protected ReviewLike() {
	}

	public ReviewLike(Review review, User user) {
		this.review = review;
		this.user = user;
	}

	public Long getId() {
		return this.id;
	}

	public Review getReview() {
		return this.review;
	}

	public User getUser() {
		return this.user;
	}

}
