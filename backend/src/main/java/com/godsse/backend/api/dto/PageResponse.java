package com.godsse.backend.api.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 목록 응답의 공통 형태(페이지네이션).
 *
 * <p>
 * 화면에서 "더 보기"나 페이지 번호를 만들 때 필요한 값을 함께 내려준다.
 *
 * @param items      현재 페이지의 항목 목록
 * @param page       현재 페이지 번호(0부터 시작)
 * @param size       페이지 크기(한 번에 가져온 개수)
 * @param total      전체 항목 수
 * @param totalPages 전체 페이지 수
 * @param hasNext    다음 페이지가 있는지
 * @param <T>        항목 타입
 */
public record PageResponse<T>(List<T> items, int page, int size, long total, int totalPages, boolean hasNext) {

	/** Spring Data 의 {@code Page} 를 응답 DTO 로 바꾼다. */
	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages(), page.hasNext());
	}

}
