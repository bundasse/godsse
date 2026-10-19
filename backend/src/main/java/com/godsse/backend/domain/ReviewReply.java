package com.godsse.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 감상에 달린 답장 한 건.
 *
 * <p>
 * 답장은 <b>한 단계</b>만 둔다(답장에 또 답장하는 구조는 만들지 않는다). 대화가 길어지면
 * 감상 상세 화면이 복잡해지고, 30~50명 규모의 롤링페이퍼 성격에는 한 단계로 충분하다.
 */
@Entity
@Table(name = "review_replies", indexes = { @Index(name = "idx_review_replies_review", columnList = "review_id") })
public class ReviewReply extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	/** 답장 내용(500자 이하). */
	@Column(nullable = false, length = 500)
	private String body;

	protected ReviewReply() {
	}

	public ReviewReply(Review review, User author, String body) {
		this.review = review;
		this.author = author;
		this.body = body;
	}

	public Long getId() {
		return this.id;
	}

	public Review getReview() {
		return this.review;
	}

	public User getAuthor() {
		return this.author;
	}

	public String getBody() {
		return this.body;
	}

}
