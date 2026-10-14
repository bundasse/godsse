package com.godsse.backend.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.godsse.backend.api.dto.PageResponse;
import com.godsse.backend.api.dto.ReviewCreateRequest;
import com.godsse.backend.api.dto.ReviewResponse;
import com.godsse.backend.api.dto.ReviewSummary;
import com.godsse.backend.api.dto.ReviewUpdateRequest;
import com.godsse.backend.common.LoginUser;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.ReviewService;

import jakarta.validation.Valid;

/**
 * 감상(리뷰) 작성·조회·수정·삭제.
 *
 * <p>
 * 목록은 한 엔드포인트에서 조건으로 나눈다.
 * <ul>
 * <li>{@code GET /api/reviews} — 홈 피드(전체 공개 최신순)</li>
 * <li>{@code GET /api/reviews?author=핸들} — 그 사람이 쓴 감상</li>
 * <li>{@code GET /api/reviews?recipient=핸들} — 그 사람이 받은 감상</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

	private static final int MAX_PAGE_SIZE = 50;

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	/**
	 * 감상 작성. 성공하면 201.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewResponse create(@LoginUser User user, @Valid @RequestBody ReviewCreateRequest request) {
		return this.reviewService.create(user, request);
	}

	/**
	 * 감상 목록. 로그인하지 않아도 볼 수 있다(공개 범위 안에서).
	 */
	@GetMapping
	public PageResponse<ReviewSummary> list(@LoginUser(required = false) User viewer,
			@RequestParam(name = "author", required = false) String author,
			@RequestParam(name = "recipient", required = false) String recipient,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {

		Pageable pageable = toPageable(page, size);

		if (author != null && !author.isBlank()) {
			return PageResponse.of(this.reviewService.written(author.trim(), viewer, pageable));
		}
		if (recipient != null && !recipient.isBlank()) {
			return PageResponse.of(this.reviewService.received(recipient.trim(), viewer, pageable));
		}
		return PageResponse.of(this.reviewService.feed(pageable));
	}

	/**
	 * 감상 한 건(전문). 공개 범위 밖이면 403.
	 */
	@GetMapping("/{id}")
	public ReviewResponse get(@PathVariable Long id, @LoginUser(required = false) User viewer) {
		return this.reviewService.get(id, viewer);
	}

	/**
	 * 감상 수정. 작성자만 가능하다(아니면 403).
	 */
	@PatchMapping("/{id}")
	public ReviewResponse update(@LoginUser User user, @PathVariable Long id,
			@Valid @RequestBody ReviewUpdateRequest request) {
		return this.reviewService.update(user, id, request);
	}

	/**
	 * 감상 삭제. 작성자만 가능하다(아니면 403). 성공하면 204.
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@LoginUser User user, @PathVariable Long id) {
		this.reviewService.delete(user, id);
	}

	/** 정렬은 저장소 쿼리 안에 있으므로 여기서는 페이지 정보만 만든다. */
	private static Pageable toPageable(int page, int size) {
		return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
	}

}
