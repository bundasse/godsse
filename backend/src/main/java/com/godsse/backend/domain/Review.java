package com.godsse.backend.domain;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.godsse.backend.domain.enums.SpoilerMode;
import com.godsse.backend.domain.enums.Visibility;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
 * 감상 메시지(리뷰) 한 건. "작성자가 받은 사람에게, 특정 세션에 대해 남기는 글"이다.
 *
 * <p>
 * 세션 정보(플레이 날짜·룰·시나리오)를 리뷰가 직접 가지고 있다. 별도의 "세션" 테이블을 두지 않는
 * 단순한 구조이며, 나중에 여러 사람의 리뷰를 하나의 세션으로 묶고 싶어지면 그때 세션 테이블을 추가한다.
 *
 * <p>
 * 본문({@code body})에는 스포일러 마커가 그대로 들어 있을 수 있다.
 * <pre>
 *   오늘 정말 즐거웠습니다. ||범인은 집사였습니다.|| 다음에도 또 해요!
 *   :::spoiler
 *   여기서부터는 결말 스포일러입니다.
 * </pre>
 * 마커를 문자 그대로 저장하고 화면에서 해석한다. 그래야 작성자가 글을 수정할 때 위치를 다시 계산할 필요가 없다.
 */
@Entity
@Table(name = "reviews",
		indexes = { @Index(name = "idx_reviews_recipient", columnList = "recipient_id"),
				@Index(name = "idx_reviews_author", columnList = "author_id") })
public class Review extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 리뷰를 쓴 사람. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	/** 리뷰를 받는 사람(함께 플레이한 상대). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	/** 플레이한 날짜(필수). */
	@Column(nullable = false)
	private LocalDate playedOn;

	/** 사용한 룰(필수). 예: {@code 크툴루의 부름 7판}. */
	@Column(nullable = false, length = 50)
	private String ruleName;

	/** 시나리오명(선택). */
	@Column(length = 100)
	private String scenarioName;

	/** 본문(필수, 100자 이상은 요청 DTO 에서 검증). */
	@Column(nullable = false, length = 10000)
	private String body;

	/** 리뷰 전체 스포일러 표시 방식. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SpoilerMode spoilerMode = SpoilerMode.NONE;

	/** 공개 범위. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Visibility visibility = Visibility.PUBLIC;

	/**
	 * 감정 태그(예: {@code RP}, {@code 전투}, {@code 호러}).
	 *
	 * <p>
	 * {@code @ElementCollection} 은 별도 엔티티를 만들지 않고 값 목록을 저장하는 방법이다.
	 * {@code review_tags} 테이블에 (review_id, tag_code) 행으로 들어간다.
	 */
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
	@Column(name = "tag_code", nullable = false, length = 30)
	private Set<String> tags = new LinkedHashSet<>();

	protected Review() {
	}

	public Review(User author, User recipient, LocalDate playedOn, String ruleName, String scenarioName, String body,
			SpoilerMode spoilerMode, Visibility visibility, Set<String> tags) {
		this.author = author;
		this.recipient = recipient;
		apply(playedOn, ruleName, scenarioName, body, spoilerMode, visibility, tags);
	}

	/** 작성 내용을 한 번에 반영한다(작성·수정 공용). */
	public void apply(LocalDate playedOn, String ruleName, String scenarioName, String body, SpoilerMode spoilerMode,
			Visibility visibility, Set<String> tags) {
		this.playedOn = playedOn;
		this.ruleName = ruleName;
		this.scenarioName = scenarioName;
		this.body = body;
		this.spoilerMode = spoilerMode;
		this.visibility = visibility;
		this.tags.clear();
		if (tags != null) {
			this.tags.addAll(tags);
		}
	}

	public Long getId() {
		return this.id;
	}

	public User getAuthor() {
		return this.author;
	}

	public User getRecipient() {
		return this.recipient;
	}

	public LocalDate getPlayedOn() {
		return this.playedOn;
	}

	public String getRuleName() {
		return this.ruleName;
	}

	public String getScenarioName() {
		return this.scenarioName;
	}

	public String getBody() {
		return this.body;
	}

	public SpoilerMode getSpoilerMode() {
		return this.spoilerMode;
	}

	public Visibility getVisibility() {
		return this.visibility;
	}

	/** 태그 목록(수정 가능한 내부 컬렉션을 그대로 노출하지 않도록 복사본을 돌려준다). */
	public Set<String> getTags() {
		return new LinkedHashSet<>(this.tags);
	}

}
