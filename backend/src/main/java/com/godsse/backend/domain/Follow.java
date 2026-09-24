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
 * 팔로우 관계 한 건. "누가(follower) 누구를(following) 팔로우한다".
 *
 * <p>
 * 같은 조합이 두 번 저장되지 않도록 {@code (follower_id, following_id)} 에 유니크 제약을 둔다.
 * (DB 차원의 안전장치이며, 서비스에서도 중복을 확인해 409 응답을 준다.)
 */
@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(name = "uk_follows_pair",
		columnNames = { "follower_id", "following_id" }))
public class Follow extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 팔로우를 거는 사람. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "follower_id", nullable = false)
	private User follower;

	/** 팔로우를 받는 사람. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "following_id", nullable = false)
	private User following;

	protected Follow() {
	}

	public Follow(User follower, User following) {
		this.follower = follower;
		this.following = following;
	}

	public Long getId() {
		return this.id;
	}

	public User getFollower() {
		return this.follower;
	}

	public User getFollowing() {
		return this.following;
	}

}
