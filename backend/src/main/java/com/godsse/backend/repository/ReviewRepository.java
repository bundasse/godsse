package com.godsse.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.godsse.backend.domain.Review;
import com.godsse.backend.domain.enums.Visibility;

/**
 * 감상(리뷰) 조회/저장.
 *
 * <p>
 * <b>공개 범위</b>를 목록에서도 지켜야 하므로, 목록 쿼리는 "조회자가 볼 수 있는 것"이라는 조건을 함께 쓴다.
 * 조건은 아래 세 가지다.
 *
 * <ul>
 * <li>{@code PUBLIC} — 누구나</li>
 * <li>{@code FOLLOWERS} — 작성자를 팔로우한 사람</li>
 * <li>{@code PRIVATE} — 작성자와 받은 사람만</li>
 * </ul>
 *
 * <p>
 * {@code :viewerId} 에 null(비로그인)을 넘기면 {@code r.author.id = null} 이 되어 어떤 행과도 참이 되지 않는다.
 * 덕분에 로그인/비로그인을 같은 쿼리로 처리할 수 있다.
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

	/** 특정 사용자가 받은 감상 중 조회자가 볼 수 있는 것(마이페이지 카드). */
	@Query("""
			select r from Review r
			where r.recipient.id = :userId
			  and (r.visibility = :publicVisibility
			       or r.author.id = :viewerId
			       or r.recipient.id = :viewerId
			       or (r.visibility = :followersVisibility
			           and exists (select 1 from Follow f
			                       where f.follower.id = :viewerId and f.following.id = r.author.id)))
			order by r.playedOn desc, r.createdAt desc
			""")
	Page<Review> findReceivedVisibleTo(@Param("userId") Long userId, @Param("viewerId") Long viewerId,
			@Param("publicVisibility") Visibility publicVisibility,
			@Param("followersVisibility") Visibility followersVisibility, Pageable pageable);

	/** 특정 사용자가 쓴 감상 중 조회자가 볼 수 있는 것. */
	@Query("""
			select r from Review r
			where r.author.id = :userId
			  and (r.visibility = :publicVisibility
			       or r.author.id = :viewerId
			       or r.recipient.id = :viewerId
			       or (r.visibility = :followersVisibility
			           and exists (select 1 from Follow f
			                       where f.follower.id = :viewerId and f.following.id = r.author.id)))
			order by r.playedOn desc, r.createdAt desc
			""")
	Page<Review> findWrittenVisibleTo(@Param("userId") Long userId, @Param("viewerId") Long viewerId,
			@Param("publicVisibility") Visibility publicVisibility,
			@Param("followersVisibility") Visibility followersVisibility, Pageable pageable);

	/** 홈 피드 — 전체 공개 감상만, 플레이 날짜 최신순. */
	Page<Review> findByVisibilityOrderByPlayedOnDescCreatedAtDesc(Visibility visibility, Pageable pageable);

	/** 받은 리뷰 개수(공개 범위와 무관한 전체 수). */
	long countByRecipientId(Long recipientId);

	/** 쓴 리뷰 개수(공개 범위와 무관한 전체 수). */
	long countByAuthorId(Long authorId);

}
