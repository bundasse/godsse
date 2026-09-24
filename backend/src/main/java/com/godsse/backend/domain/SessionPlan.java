package com.godsse.backend.domain;

import java.time.LocalDate;

import com.godsse.backend.domain.enums.SessionStatus;

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
 * 마이페이지 달력에 기록하는 "내가 플레이할(또는 플레이한) 세션 일정".
 *
 * <p>
 * 리뷰({@link Review})와는 별개다. 리뷰는 상대에게 남기는 감상이고, 이 일정은 나만 보는 계획표다.
 * 플레이를 마치면 이 일정을 보고 리뷰를 쓰러 갈 수 있도록 화면에서 연결해 준다.
 */
@Entity
@Table(name = "session_plans", indexes = { @Index(name = "idx_session_plans_owner_date", columnList = "owner_id,planned_date") })
public class SessionPlan extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 일정을 소유한 사용자. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	/** 일정 제목(예: {@code 정기 CoC 캠페인 3화}). */
	@Column(nullable = false, length = 60)
	private String title;

	/** 예정(또는 플레이한) 날짜. */
	@Column(nullable = false)
	private LocalDate plannedDate;

	/** 사용할 룰(필수). */
	@Column(nullable = false, length = 50)
	private String ruleName;

	/** 시나리오명(선택). */
	@Column(length = 100)
	private String scenarioName;

	/** 내가 GM(진행자)인지 여부. */
	@Column(nullable = false)
	private boolean gm;

	/** 메모(선택). 모집 여부, 장소, 준비물 등. */
	@Column(length = 1000)
	private String memo;

	/** 일정 상태. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SessionStatus status = SessionStatus.PLANNED;

	protected SessionPlan() {
	}

	public SessionPlan(User owner, String title, LocalDate plannedDate, String ruleName, String scenarioName,
			boolean gm, String memo) {
		this.owner = owner;
		apply(title, plannedDate, ruleName, scenarioName, gm, memo);
	}

	/** 일정 내용을 한 번에 반영한다(작성·수정 공용). */
	public void apply(String title, LocalDate plannedDate, String ruleName, String scenarioName, boolean gm,
			String memo) {
		this.title = title;
		this.plannedDate = plannedDate;
		this.ruleName = ruleName;
		this.scenarioName = scenarioName;
		this.gm = gm;
		this.memo = memo;
	}

	/** 상태만 바꾼다(예정 → 완료/취소). */
	public void changeStatus(SessionStatus status) {
		this.status = status;
	}

	public Long getId() {
		return this.id;
	}

	public User getOwner() {
		return this.owner;
	}

	public String getTitle() {
		return this.title;
	}

	public LocalDate getPlannedDate() {
		return this.plannedDate;
	}

	public String getRuleName() {
		return this.ruleName;
	}

	public String getScenarioName() {
		return this.scenarioName;
	}

	public boolean isGm() {
		return this.gm;
	}

	public String getMemo() {
		return this.memo;
	}

	public SessionStatus getStatus() {
		return this.status;
	}

}
