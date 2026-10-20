package com.godsse.backend.domain;

import java.time.Instant;

import com.godsse.backend.domain.enums.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 알림 한 건. "누가(actor) 누구에게(recipient) 무슨 일(type)이 있었는지"를 남긴다.
 *
 * <p>
 * <b>받는 사람과 행동한 사람이 같으면 알림을 만들지 않는다.</b> (내가 내 감상에 좋아요를 눌러도 알림 없음)
 *
 * <p>
 * 좋아요·언팔로우처럼 <b>되돌릴 수 있는 행동</b>은 알림도 함께 지운다.
 * (취소했는데 "좋아요를 눌렀습니다" 알림이 남아 있으면 사실과 달라진다)
 */
@Entity
@Table(name = "notifications", indexes = {
		@Index(name = "idx_notifications_recipient", columnList = "recipient_id,created_at"),
		@Index(name = "idx_notifications_unread", columnList = "recipient_id,read_at") })
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 알림을 받는 사람. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	/** 그 일을 한 사람. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false)
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationType type;

	/** 관련 감상. FOLLOW 알림은 null. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	/** 읽은 시각. null 이면 아직 안 읽은 알림이다. */
	@Column(name = "read_at")
	private Instant readAt;

	protected Notification() {
	}

	public Notification(User recipient, User actor, NotificationType type, Review review) {
		this.recipient = recipient;
		this.actor = actor;
		this.type = type;
		this.review = review;
	}

	/** 읽음으로 표시한다. 이미 읽었으면 시각을 바꾸지 않는다. */
	public void markRead() {
		if (this.readAt == null) {
			this.readAt = Instant.now();
		}
	}

	public boolean isRead() {
		return this.readAt != null;
	}

	public Long getId() {
		return this.id;
	}

	public User getRecipient() {
		return this.recipient;
	}

	public User getActor() {
		return this.actor;
	}

	public NotificationType getType() {
		return this.type;
	}

	public Review getReview() {
		return this.review;
	}

	public Instant getReadAt() {
		return this.readAt;
	}

}
