package com.godsse.backend.domain;

import jakarta.persistence.Column;
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
 * 사용자가 직접 고른 룰 색(히트맵 개인화).
 *
 * <p>
 * 룰은 {@code session_plans.rule_name} 처럼 <b>이름(문자열)</b> 으로 저장된다.
 * 그래서 색도 룰 테이블이 아니라 <b>이름 + 사용자</b> 조합으로 남긴다.
 * (같은 이름의 룰이라도 사람마다 색을 다르게 고를 수 있다)
 *
 * <p>
 * 한 사용자가 같은 룰에 색을 두 줄 저장하면 어느 색을 써야 할지 알 수 없으므로
 * {@code (user_id, rule_name)} 에 유니크 제약을 둔다. 저장은 \"있으면 바꾸고, 없으면 만든다\"(업서트) 방식이다.
 *
 * <p>
 * 기본 팔레트 색은 저장하지 않는다. 사용자가 고른 색만 남기고, 나머지는
 * {@link com.godsse.backend.common.RuleColors#defaultColor(String)} 로 계산한다.
 * (룰이 늘어도 행이 쌓이지 않고, 기본색 계산 규칙을 바꿔도 저장 데이터가 낡지 않는다)
 */
@Entity
@Table(name = "rule_colors",
		uniqueConstraints = @UniqueConstraint(name = "uk_rule_colors_user_rule",
				columnNames = { "user_id", "rule_name" }))
public class RuleColor extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 색을 고른 사용자. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** 색을 고른 룰 이름(일정에 저장된 문자열 그대로). */
	@Column(name = "rule_name", nullable = false, length = 50)
	private String ruleName;

	/** 화면에 쓸 색({@code #rrggbb}, 소문자). */
	@Column(nullable = false, length = 7)
	private String color;

	protected RuleColor() {
	}

	public RuleColor(User user, String ruleName, String color) {
		this.user = user;
		this.ruleName = ruleName;
		this.color = color;
	}

	/** 색만 바꾼다(같은 룰에 다시 저장할 때). */
	public void changeColor(String color) {
		this.color = color;
	}

	public Long getId() {
		return this.id;
	}

	public User getUser() {
		return this.user;
	}

	public String getRuleName() {
		return this.ruleName;
	}

	public String getColor() {
		return this.color;
	}

}
