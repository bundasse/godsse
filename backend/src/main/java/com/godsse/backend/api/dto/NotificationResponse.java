package com.godsse.backend.api.dto;

import java.time.Instant;

import com.godsse.backend.domain.Notification;
import com.godsse.backend.domain.Review;
import com.godsse.backend.domain.enums.NotificationType;

/**
 * 알림 한 건.
 *
 * @param id        알림 id
 * @param type      종류 (REVIEW·LIKE·REPLY·FOLLOW)
 * @param actor     그 일을 한 사람
 * @param reviewId  관련 감상 id (FOLLOW 는 null)
 * @param read      읽었는지
 * @param createdAt 만들어진 시각
 */
public record NotificationResponse(Long id, NotificationType type, UserSummary actor, Long reviewId, boolean read,
		Instant createdAt) {

	/** 엔티티를 화면용 DTO 로 바꾼다. */
	public static NotificationResponse of(Notification notification) {
		Review review = notification.getReview();

		return new NotificationResponse(notification.getId(), notification.getType(),
				UserSummary.of(notification.getActor()), review == null ? null : review.getId(),
				notification.isRead(), notification.getCreatedAt());
	}

}
