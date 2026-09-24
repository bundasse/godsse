package com.godsse.backend.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * 생성 시각과 수정 시각을 공통으로 관리하는 상위 클래스.
 *
 * <p>
 * {@code @MappedSuperclass} 는 "이 클래스는 테이블이 되지 않고, 자식 엔티티들이 컬럼만 물려받는다"는 뜻이다.
 * 프론트엔드에 비유하면 공통 mixin(공용 스타일/공용 컴포넌트)과 비슷하다.
 *
 * <p>
 * 시각 값은 JPA 콜백({@code @PrePersist}: 저장 직전, {@code @PreUpdate}: 수정 직전)에서 채운다.
 */
@MappedSuperclass
public abstract class BaseTimeEntity {

	/** 저장된 시각. 한 번 저장되면 바뀌지 않는다. */
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	/** 마지막으로 수정된 시각. */
	@Column(nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

}
