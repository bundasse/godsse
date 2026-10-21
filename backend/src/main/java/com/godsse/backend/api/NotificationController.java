package com.godsse.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.godsse.backend.api.dto.NotificationListResponse;
import com.godsse.backend.api.dto.UnreadCountResponse;
import com.godsse.backend.common.LoginUser;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.NotificationService;

/**
 * 알림 조회·읽음 처리. 모두 <b>로그인 필요</b>(미로그인 401).
 *
 * <ul>
 * <li>{@code GET /api/notifications} — 내 알림 목록 + 안 읽은 개수</li>
 * <li>{@code GET /api/notifications/unread-count} — 안 읽은 개수만(헤더 배지)</li>
 * <li>{@code GET /api/notifications/stream} — 실시간 구독(SSE)</li>
 * <li>{@code POST /api/notifications/{id}/read} — 하나 읽음</li>
 * <li>{@code POST /api/notifications/read-all} — 모두 읽음</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	/** 내 알림 목록(최신순 30건)과 안 읽은 개수. */
	@GetMapping
	public NotificationListResponse list(@LoginUser User loginUser) {
		return this.notificationService.list(loginUser);
	}

	/** 안 읽은 개수만. */
	@GetMapping("/unread-count")
	public UnreadCountResponse unreadCount(@LoginUser User loginUser) {
		return this.notificationService.unreadCount(loginUser);
	}

	/**
	 * 실시간 알림 구독(SSE).
	 *
	 * <p>
	 * 응답은 {@code text/event-stream} 이고, 연결 직후 {@code connected}(현재 개수)를,
	 * 이후 개수가 바뀔 때마다 {@code changed} 를 보낸다. 둘 다 데이터는 {@code {"count": n}} 이다.
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@LoginUser User loginUser) {
		return this.notificationService.subscribe(loginUser);
	}

	/** 알림 하나를 읽음으로 표시한다. */
	@PostMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void read(@LoginUser User loginUser, @PathVariable Long id) {
		this.notificationService.markRead(loginUser, id);
	}

	/** 내 알림을 모두 읽음으로 표시한다. */
	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void readAll(@LoginUser User loginUser) {
		this.notificationService.markAllRead(loginUser);
	}

}
