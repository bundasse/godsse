package com.godsse.backend.api.dto;

import java.util.List;

/**
 * 알림 목록 응답. 목록과 함께 안 읽은 개수를 내려 주어 화면이 한 번에 그릴 수 있게 한다.
 *
 * @param items       최근 알림(최신순, 최대 30건)
 * @param unreadCount 안 읽은 알림 수
 */
public record NotificationListResponse(List<NotificationResponse> items, long unreadCount) {
}
