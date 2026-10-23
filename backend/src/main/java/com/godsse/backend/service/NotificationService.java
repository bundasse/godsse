package com.godsse.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.godsse.backend.api.dto.NotificationListResponse;
import com.godsse.backend.api.dto.NotificationResponse;
import com.godsse.backend.api.dto.UnreadCountResponse;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.domain.Notification;
import com.godsse.backend.domain.Review;
import com.godsse.backend.domain.User;
import com.godsse.backend.domain.enums.NotificationType;
import com.godsse.backend.repository.NotificationRepository;

/**
 * 알림 만들기·조회·읽음 처리.
 *
 * <p>
 * 알림은 다른 기능의 <b>곁다리</b>로 만들어진다. 그래서 "언제 만들지"보다
 * <b>"언제 만들지 말지"</b>가 중요하다.
 * <ul>
 * <li>받는 사람과 행동한 사람이 같으면 만들지 않는다(내 활동으로 나를 깨우지 않는다).</li>
 * <li>좋아요 취소·언팔로우처럼 <b>되돌린 행동</b>은 알림도 지운다(사실과 다른 알림을 남기지 않는다).</li>
 * </ul>
 *
 * <p>
 * 개수가 바뀌면 <b>사건만 발행</b>하고, 실제 SSE 전송은 커밋 이후
 * {@link NotificationPushListener} 가 한다. (롤백된 변경을 화면에 보내지 않기 위해)
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

	/** 화면에 한 번에 보여 줄 최근 알림 수. */
	private static final int LIST_LIMIT = 30;

	private final NotificationRepository notificationRepository;
	private final NotificationStream notificationStream;
	private final ApplicationEventPublisher eventPublisher;

	public NotificationService(NotificationRepository notificationRepository, NotificationStream notificationStream,
			ApplicationEventPublisher eventPublisher) {
		this.notificationRepository = notificationRepository;
		this.notificationStream = notificationStream;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * 알림을 남긴다.
	 *
	 * <p>
	 * 받는 사람이 없거나(FOLOW 대상 없음 같은 경우) 행동한 사람과 같으면 아무 일도 하지 않는다.
	 *
	 * @param recipient 받는 사람
	 * @param actor     그 일을 한 사람
	 * @param type      종류
	 * @param review    관련 감상 (FOLLOW 는 null)
	 */
	@Transactional
	public void notify(User recipient, User actor, NotificationType type, Review review) {
		if (recipient == null || actor == null || recipient.getId().equals(actor.getId())) {
			return;
		}

		this.notificationRepository.save(new Notification(recipient, actor, type, review));

		// 커밋이 끝나면 열려 있는 화면에 알려 준다(없으면 DB 에만 남는다).
		publishCountChanged(recipient.getId());
	}

	/** 내 알림 목록(최신순 30건)과 안 읽은 개수. */
	public NotificationListResponse list(User loginUser) {
		List<NotificationResponse> items = this.notificationRepository
			.findByRecipientIdOrderByCreatedAtDescIdDesc(loginUser.getId(), PageRequest.of(0, LIST_LIMIT))
			.stream()
			.map(NotificationResponse::of)
			.toList();

		return new NotificationListResponse(items, unreadCountOf(loginUser.getId()));
	}

	/** 안 읽은 알림 수(헤더 배지). */
	public UnreadCountResponse unreadCount(User loginUser) {
		return new UnreadCountResponse(unreadCountOf(loginUser.getId()));
	}

	/**
	 * 실시간 구독을 연다. 연결되자마자 현재 개수를 한 번 보내 화면과 서버를 맞춘다.
	 */
	public SseEmitter subscribe(User loginUser) {
		return this.notificationStream.subscribe(loginUser.getId(), unreadCountOf(loginUser.getId()));
	}

	/**
	 * 알림 하나를 읽음으로 표시한다.
	 *
	 * @throws NotFoundException 없거나 남의 알림일 때 (404) — 남의 알림의 존재 여부를 알려 주지 않는다
	 */
	@Transactional
	public void markRead(User loginUser, Long id) {
		Notification notification = this.notificationRepository.findById(id)
			.filter(item -> item.getRecipient().getId().equals(loginUser.getId()))
			.orElseThrow(() -> new NotFoundException("알림을 찾을 수 없습니다."));

		notification.markRead();

		// 읽음도 "개수가 바뀐 일"이라 다른 탭/창에 알려 준다.
		publishCountChanged(loginUser.getId());
	}

	/** 내 알림을 모두 읽음으로 표시한다(한 번의 UPDATE). */
	@Transactional
	public void markAllRead(User loginUser) {
		this.notificationRepository.markAllRead(loginUser.getId(), Instant.now());
		publishCountChanged(loginUser.getId());
	}

	/** 좋아요 취소 — 그 좋아요 알림을 지운다. */
	@Transactional
	public void removeLike(User actor, Review review) {
		this.notificationRepository.deleteByTypeAndReviewIdAndActorId(NotificationType.LIKE, review.getId(),
				actor.getId());

		User recipient = review.getAuthor();
		publishCountChanged(recipient.getId());
	}

	/** 언팔로우 — 그 팔로우 알림을 지운다. */
	@Transactional
	public void removeFollow(User actor, User recipient) {
		this.notificationRepository.deleteByTypeAndRecipientIdAndActorId(NotificationType.FOLLOW, recipient.getId(),
				actor.getId());
		publishCountChanged(recipient.getId());
	}

	/**
	 * "개수가 바뀌었다"는 사건을 발행한다.
	 *
	 * <p>
	 * 지금 개수를 <b>트랜잭션 안에서</b> 세어 사건에 담는다. 커밋 이후에 다시 세면
	 * 이 트랜잭션의 변경이 반영되었는지 보장할 수 없기 때문이다.
	 */
	private void publishCountChanged(Long userId) {
		this.eventPublisher.publishEvent(new NotificationCountChangedEvent(userId, unreadCountOf(userId)));
	}

	private long unreadCountOf(Long userId) {
		return this.notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
	}

}
