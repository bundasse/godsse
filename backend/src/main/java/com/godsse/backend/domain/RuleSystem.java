package com.godsse.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 룰(규칙 체계) 목록. 리뷰 작성 화면의 자동완성 선택지로 사용한다.
 *
 * <p>
 * 리뷰는 이 테이블의 값이 아니라 {@code ruleName} 문자열을 직접 저장한다.
 * (초기에는 자유 입력을 허용하고, 데이터가 쌓이면 정규화를 검토한다.)
 */
@Entity
@Table(name = "rule_systems")
public class RuleSystem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 내부 식별 코드(예: {@code COC7}). */
	@Column(nullable = false, unique = true, length = 30)
	private String code;

	/** 화면에 보이는 이름(예: {@code 크툴루의 부름 7판}). */
	@Column(nullable = false, length = 60)
	private String label;

	/** 목록 정렬 순서(작을수록 위). */
	@Column(nullable = false)
	private int sortOrder;

	/** 사용 여부. false 면 자동완성 목록에서 빠진다. */
	@Column(nullable = false)
	private boolean active = true;

	protected RuleSystem() {
	}

	public RuleSystem(String code, String label, int sortOrder) {
		this.code = code;
		this.label = label;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return this.id;
	}

	public String getCode() {
		return this.code;
	}

	public String getLabel() {
		return this.label;
	}

	public int getSortOrder() {
		return this.sortOrder;
	}

	public boolean isActive() {
		return this.active;
	}

}
