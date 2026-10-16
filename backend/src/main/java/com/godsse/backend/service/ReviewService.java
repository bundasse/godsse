package com.godsse.backend.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.ReviewCreateRequest;
import com.godsse.backend.api.dto.ReviewResponse;
import com.godsse.backend.api.dto.ReviewSummary;
import com.godsse.backend.api.dto.ReviewUpdateRequest;
import com.godsse.backend.common.exception.BadRequestException;
import com.godsse.backend.common.exception.ForbiddenException;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.common.exception.UnauthorizedException;
import com.godsse.backend.domain.Review;
import com.godsse.backend.domain.User;
import com.godsse.backend.domain.enums.SpoilerMode;
import com.godsse.backend.domain.enums.Visibility;
import com.godsse.backend.repository.FollowRepository;
import com.godsse.backend.repository.ReviewRepository;
import com.godsse.backend.repository.UserRepository;

/**
 * 감상(리뷰) 작성·조회·수정·삭제.
 *
 * <p>
 * 감상은 "누가(author) 누구에게(recipient) 어느 세션(날짜·룰·시나리오)에서 무엇을 느꼈는지"다.
 * 본문의 스포일러 마커는 <b>해석하지 않고 그대로 저장</b>한다(가리는 일은 화면에서 한다).
 *
 * <p>
 * 공개 범위는 조회와 목록 <b>양쪽</b>에서 확인한다. 목록은 저장소의 JPQL 이 걸러 주고,
 * 단건은 {@link #canView} 가 판단한다.
 */
@Service
@Transactional(readOnly = true)
public class ReviewService {

	/** 화면에서 고를 수 있는 태그 개수와 맞춘다. */
	private static final int MAX_TAGS = 8;

	private final ReviewRepository reviewRepository;
	private final UserRepository userRepository;
	private final FollowRepository followRepository;

	public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository,
			FollowRepository followRepository) {
		this.reviewRepository = reviewRepository;
		this.userRepository = userRepository;
		this.followRepository = followRepository;
	}

	/**
	 * 감상을 작성한다.
	 *
	 * @throws BadRequestException 자신에게 감상을 남기려 할 때 (400)
	 * @throws NotFoundException   받는 사람의 핸들이 없을 때 (404)
	 */
	@Transactional
	public ReviewResponse create(User loginUser, ReviewCreateRequest request) {
		User author = getUserById(loginUser.getId());
		User recipient = getUserByHandle(request.toHandle());

		if (author.getId().equals(recipient.getId())) {
			throw new BadRequestException("toHandle", "자신에게는 감상을 남길 수 없습니다.");
		}

		Review review = new Review(author, recipient, request.playedOn(), request.ruleName().trim(),
				trimToNull(request.scenarioName()), request.body(), orDefault(request.spoilerMode(), SpoilerMode.NONE),
				orDefault(request.visibility(), Visibility.PUBLIC), cleanTags(request.tags()));

		return ReviewResponse.of(this.reviewRepository.save(review), true);
	}

	/**
	 * 감상 한 건. 공개 범위 밖이면 403.
	 */
	public ReviewResponse get(Long id, User viewer) {
		Review review = getReview(id);

		if (!canView(review, viewer)) {
			throw new ForbiddenException("이 감상을 볼 수 없습니다.");
		}

		return ReviewResponse.of(review, isAuthor(review, viewer));
	}

	/**
	 * 감상을 수정한다. 작성자만 가능하다.
	 */
	@Transactional
	public ReviewResponse update(User loginUser, Long id, ReviewUpdateRequest request) {
		Review review = getReview(id);
		requireAuthor(review, loginUser);

		review.apply(request.playedOn(), request.ruleName().trim(), trimToNull(request.scenarioName()), request.body(),
				orDefault(request.spoilerMode(), SpoilerMode.NONE),
				orDefault(request.visibility(), Visibility.PUBLIC), cleanTags(request.tags()));

		return ReviewResponse.of(review, true);
	}

	/**
	 * 감상을 삭제한다. 작성자만 가능하다.
	 */
	@Transactional
	public void delete(User loginUser, Long id) {
		Review review = getReview(id);
		requireAuthor(review, loginUser);

		this.reviewRepository.delete(review);
	}

	/** 이 사람이 받은 감상 중 조회자가 볼 수 있는 것. */
	public Page<ReviewSummary> received(String handle, User viewer, Pageable pageable) {
		User user = getUserByHandle(handle);

		return this.reviewRepository
			.findReceivedVisibleTo(user.getId(), viewerId(viewer), Visibility.PUBLIC, Visibility.FOLLOWERS, pageable)
			.map(ReviewSummary::of);
	}

	/** 이 사람이 쓴 감상 중 조회자가 볼 수 있는 것. */
	public Page<ReviewSummary> written(String handle, User viewer, Pageable pageable) {
		User user = getUserByHandle(handle);

		return this.reviewRepository
			.findWrittenVisibleTo(user.getId(), viewerId(viewer), Visibility.PUBLIC, Visibility.FOLLOWERS, pageable)
			.map(ReviewSummary::of);
	}

	/** 홈 피드 — 전체 공개 감상만. */
	public Page<ReviewSummary> feed(Pageable pageable) {
		return this.reviewRepository
			.findByVisibilityOrderByPlayedOnDescCreatedAtDesc(Visibility.PUBLIC, pageable)
			.map(ReviewSummary::of);
	}

	/**
	 * 팔로우 피드 — 내가 팔로우한 사람들의 감상.
	 *
	 * @throws UnauthorizedException 로그인하지 않았을 때 (401)
	 */
	public Page<ReviewSummary> feedFollowing(User viewer, Pageable pageable) {
		if (viewer == null) {
			throw new UnauthorizedException("팔로우 피드는 로그인이 필요합니다.");
		}

		return this.reviewRepository
			.findFeedVisibleTo(viewer.getId(), Visibility.PUBLIC, Visibility.FOLLOWERS, pageable)
			.map(ReviewSummary::of);
	}

	/**
	 * 조회자가 이 감상을 볼 수 있는지 판단한다.
	 *
	 * <ul>
	 * <li>PUBLIC — 누구나</li>
	 * <li>FOLLOWERS — 작성자를 팔로우한 사람 (+ 작성자·받은 사람)</li>
	 * <li>PRIVATE — 작성자와 받은 사람만</li>
	 * </ul>
	 */
	private boolean canView(Review review, User viewer) {
		if (review.getVisibility() == Visibility.PUBLIC) {
			return true;
		}
		if (viewer == null) {
			return false;
		}
		if (isAuthor(review, viewer) || review.getRecipient().getId().equals(viewer.getId())) {
			return true;
		}
		if (review.getVisibility() == Visibility.FOLLOWERS) {
			return this.followRepository.existsByFollowerIdAndFollowingId(viewer.getId(),
					review.getAuthor().getId());
		}
		return false;
	}

	private void requireAuthor(Review review, User loginUser) {
		if (!isAuthor(review, loginUser)) {
			throw new ForbiddenException("이 감상을 수정하거나 삭제할 권한이 없습니다.");
		}
	}

	private static boolean isAuthor(Review review, User user) {
		return user != null && review.getAuthor().getId().equals(user.getId());
	}

	private static Long viewerId(User viewer) {
		return viewer == null ? null : viewer.getId();
	}

	/** 값이 없으면 기본값을 쓴다. (요청에서 스포일러 모드·공개 범위를 생략할 수 있게) */
	private static <T> T orDefault(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}

	/** 태그는 앞뒤 공백 제거·빈 값 제거 후 최대 8개만 남긴다. */
	private static Set<String> cleanTags(Set<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return null;
		}

		Set<String> cleaned = new LinkedHashSet<>();
		for (String tag : tags) {
			if (tag == null) {
				continue;
			}
			String trimmed = tag.trim();
			if (!trimmed.isEmpty()) {
				cleaned.add(trimmed.length() > 30 ? trimmed.substring(0, 30) : trimmed);
			}
			if (cleaned.size() >= MAX_TAGS) {
				break;
			}
		}

		return cleaned.isEmpty() ? null : cleaned;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private Review getReview(Long id) {
		return this.reviewRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("감상을 찾을 수 없습니다."));
	}

	private User getUserById(Long id) {
		return this.userRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
	}

	private User getUserByHandle(String handle) {
		return this.userRepository.findByHandle(handle)
			.orElseThrow(() -> new NotFoundException("toHandle", "해당 핸들의 사용자를 찾을 수 없습니다."));
	}

}
