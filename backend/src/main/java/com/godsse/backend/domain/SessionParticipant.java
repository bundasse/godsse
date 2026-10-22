package com.godsse.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 세션 일정에 함께한 사람 한 명.
 *
 * <p>
 * 일정({@link SessionPlan})은 나만 보는 계획표지만, "누구와 함께했는지"는 나중에 감상을 남길 때 필요하다.
 * 그래서 일정에 사용자를 연결해 두고, 화면에서는 그 사람의 프로필과 감상 작성 링크로 이어 준다.
 *
 * <p>
 * 같은 사람을 한 일정에 두 번 넣을 수 없도록 {@code (session_plan_id, user_id)} 에 유니크 제약을 둔다.
 * (서비스에서도 확인해 409 를 주지만, DB 제약이 마지막 안전장치다)
 */
@Entity
@Table(name = "session_participants",
		uniqueConstraints = @UniqueConstraint(name = "uk_session_participants", columnNames = { "session_plan_id",
				"user_id" }),
		indexes = { @Index(name = "idx_session_participants_plan", columnList = "session_plan_id") })
public class SessionParticipant extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_plan_id", nullable = false)
	private SessionPlan plan;

	/** 함께한 사람. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	protected SessionParticipant() {
	}

	public SessionParticipant(SessionPlan plan, User user) {
		this.plan = plan;
		this.user = user;
	}

	public Long getId() {
		return this.id;
	}

	public SessionPlan getPlan() {
		return this.plan;
	}

	public User getUser() {
		return this.user;
	}

}
