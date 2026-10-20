package com.godsse.backend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.godsse.backend.domain.Notification;
import com.godsse.backend.domain.enums.NotificationType;

/**
 * 알림 조회/저장.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/** 내 알림 목록(최신순). */
	List<Notification> findByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId, Pageable pageable);

	/** 안 읽은 알림 수. */
	long countByRecipientIdAndReadAtIsNull(Long recipientId);

	/** 내 알림을 모두 읽음으로 표시한다(한 번의 UPDATE). */
	@Modifying(clearAutomatically = true)
	@Query("update Notification n set n.readAt = :now where n.recipient.id = :recipientId and n.readAt is null")
	int markAllRead(@Param("recipientId") Long recipientId, @Param("now") Instant now);

	/** 좋아요 취소 시 그 알림을 지운다. */
	void deleteByTypeAndReviewIdAndActorId(NotificationType type, Long reviewId, Long actorId);

	/** 언팔로우 시 그 알림을 지운다. */
	void deleteByTypeAndRecipientIdAndActorId(NotificationType type, Long recipientId, Long actorId);

}
