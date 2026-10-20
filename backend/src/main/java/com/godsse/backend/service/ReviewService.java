package com.godsse.backend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.ReviewCreateRequest;
import com.godsse.backend.api.dto.ReviewReplyRequest;
import com.godsse.backend.api.dto.ReviewReplyResponse;
import com.godsse.backend.api.dto.ReviewResponse;
import com.godsse.backend.api.dto.ReviewSummary;
import com.godsse.backend.api.dto.ReviewUpdateRequest;
import com.godsse.backend.common.exception.BadRequestException;
import com.godsse.backend.common.exception.ConflictException;
import com.godsse.backend.common.exception.ForbiddenException;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.common.exception.UnauthorizedException;
import com.godsse.backend.domain.Review;
import com.godsse.backend.domain.ReviewLike;
import com.godsse.backend.domain.ReviewReply;
import com.godsse.backend.domain.User;
import com.godsse.backend.domain.enums.NotificationType;
import com.godsse.backend.domain.enums.SpoilerMode;
import com.godsse.backend.domain.enums.Visibility;
import com.godsse.backend.repository.FollowRepository;
import com.godsse.backend.repository.ReviewLikeRepository;
import com.godsse.backend.repository.ReviewReplyRepository;
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
	private final ReviewLikeRepository reviewLikeRepository;
	private final ReviewReplyRepository reviewReplyRepository;
	private final NotificationService notificationService;

	public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository,
			FollowRepository followRepository, ReviewLikeRepository reviewLikeRepository,
			ReviewReplyRepository reviewReplyRepository, NotificationService notificationService) {
		this.reviewRepository = reviewRepository;
		this.userRepository = userRepository;
		this.followRepository = followRepository;
		this.reviewLikeRepository = reviewLikeRepository;
		this.reviewReplyRepository = reviewReplyRepository;
		this.notificationService = notificationService;
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

		Review saved = this.reviewRepository.save(review);
		this.notificationService.notify(recipient, author, NotificationType.REVIEW, saved);

		return ReviewResponse.of(saved, true, 0, false, 0);
	}

	/**
	 * 감상 한 건. 공개 범위 밖이면 403.
	 */
	public ReviewResponse get(Long id, User viewer) {
		Review review = getReview(id);

		if (!canView(review, viewer)) {
			throw new ForbiddenException("이 감상을 볼 수 없습니다.");
		}

		return toResponse(review, isAuthor(review, viewer), viewer);
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

		return toResponse(review, true, loginUser);
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

		Page<Review> page = this.reviewRepository.findReceivedVisibleTo(user.getId(), viewerId(viewer),
				Visibility.PUBLIC, Visibility.FOLLOWERS, pageable);

		return toSummaryPage(page, viewer);
	}

	/** 이 사람이 쓴 감상 중 조회자가 볼 수 있는 것. */
	public Page<ReviewSummary> written(String handle, User viewer, Pageable pageable) {
		User user = getUserByHandle(handle);

		Page<Review> page = this.reviewRepository.findWrittenVisibleTo(user.getId(), viewerId(viewer),
				Visibility.PUBLIC, Visibility.FOLLOWERS, pageable);

		return toSummaryPage(page, viewer);
	}

	/** 홈 피드 — 전체 공개 감상만. */
	public Page<ReviewSummary> feed(User viewer, Pageable pageable) {
		Page<Review> page = this.reviewRepository
			.findByVisibilityOrderByPlayedOnDescCreatedAtDesc(Visibility.PUBLIC, pageable);

		return toSummaryPage(page, viewer);
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

		Page<Review> page = this.reviewRepository.findFeedVisibleTo(viewer.getId(), Visibility.PUBLIC,
				Visibility.FOLLOWERS, pageable);

		return toSummaryPage(page, viewer);
	}

	/**
	 * 좋아요를 누른다.
	 *
	 * @throws ForbiddenException 이 감상을 볼 수 없을 때 (403)
	 * @throws ConflictException  이미 좋아요했을 때 (409)
	 */
	@Transactional
	public void like(User loginUser, Long reviewId) {
		Review review = getReview(reviewId);
		requireVisible(review, loginUser);

		if (this.reviewLikeRepository.existsByReviewIdAndUserId(reviewId, loginUser.getId())) {
			throw new ConflictException("이미 좋아요한 감상입니다.");
		}

		User liker = getUserById(loginUser.getId());
		this.reviewLikeRepository.save(new ReviewLike(review, liker));

		// 감상 작성자에게 알린다. 내 감상에 내가 누른 경우는 notify 안에서 걸러진다.
		this.notificationService.notify(review.getAuthor(), liker, NotificationType.LIKE, review);
	}

	/**
	 * 좋아요를 취소한다.
	 *
	 * @throws NotFoundException 좋아요하지 않았을 때 (404)
	 */
	@Transactional
	public void unlike(User loginUser, Long reviewId) {
		Review review = getReview(reviewId);
		requireVisible(review, loginUser);

		ReviewLike like = this.reviewLikeRepository.findByReviewIdAndUserId(reviewId, loginUser.getId())
			.orElseThrow(() -> new NotFoundException("좋아요하지 않은 감상입니다."));

		this.reviewLikeRepository.delete(like);

		// 되돌린 행동이므로 알림도 지운다.
		this.notificationService.removeLike(loginUser, review);
	}

	/** 답장 목록(오래된 순 = 대화 순서). 볼 수 없는 감상이면 403. */
	public List<ReviewReplyResponse> replies(Long reviewId, User viewer) {
		Review review = getReview(reviewId);
		requireVisible(review, viewer);

		return this.reviewReplyRepository.findByReviewIdOrderByCreatedAtAsc(reviewId)
			.stream()
			.map(reply -> ReviewReplyResponse.of(reply, isReplyAuthor(reply, viewer)))
			.toList();
	}

	/** 답장을 남긴다. 감상을 볼 수 있어야 한다. */
	@Transactional
	public ReviewReplyResponse addReply(User loginUser, Long reviewId, ReviewReplyRequest request) {
		Review review = getReview(reviewId);
		requireVisible(review, loginUser);

		User author = getUserById(loginUser.getId());
		ReviewReply reply = this.reviewReplyRepository
			.save(new ReviewReply(review, author, request.body().trim()));

		// 감상 작성자와 받은 사람 모두에게 알린다. 답장을 쓴 본인은 notify 에서 걸러진다.
		this.notificationService.notify(review.getAuthor(), author, NotificationType.REPLY, review);
		this.notificationService.notify(review.getRecipient(), author, NotificationType.REPLY, review);

		return ReviewReplyResponse.of(reply, true);
	}

	/** 답장을 삭제한다. 답장 작성자만 가능하다. */
	@Transactional
	public void deleteReply(User loginUser, Long reviewId, Long replyId) {
		ReviewReply reply = this.reviewReplyRepository.findById(replyId)
			.orElseThrow(() -> new NotFoundException("답장을 찾을 수 없습니다."));

		if (!reply.getReview().getId().equals(reviewId)) {
			throw new NotFoundException("답장을 찾을 수 없습니다.");
		}
		if (!isReplyAuthor(reply, loginUser)) {
			throw new ForbiddenException("내 답장만 삭제할 수 있습니다.");
		}

		this.reviewReplyRepository.delete(reply);
	}

	/** 볼 수 없는 감상이면 403. */
	private void requireVisible(Review review, User viewer) {
		if (!canView(review, viewer)) {
			throw new ForbiddenException("이 감상을 볼 수 없습니다.");
		}
	}

	private static boolean isReplyAuthor(ReviewReply reply, User user) {
		return user != null && reply.getAuthor().getId().equals(user.getId());
	}

	/**
	 * 목록을 요약 DTO 로 바꾸면서 좋아요 수·내 좋아요 여부를 붙인다.
	 *
	 * <p>
	 * "내가 좋아요한 감상 id" 는 페이지에 담긴 id 들을 한 번에 넘겨 1회로 가져온다(N+1 방지).
	 * 좋아요 수는 항목마다 세는데(20건이면 20회), 지금 규모에서는 단순함을 택했다.
	 * 목록이 커지면 group by 조회로 한 번에 가져오도록 바꾼다.
	 */
	private Page<ReviewSummary> toSummaryPage(Page<Review> page, User viewer) {
		Set<Long> likedIds = likedReviewIds(page.getContent(), viewer);

		return page.map(review -> ReviewSummary.of(review,
				this.reviewLikeRepository.countByReviewId(review.getId()), likedIds.contains(review.getId())));
	}

	private Set<Long> likedReviewIds(List<Review> reviews, User viewer) {
		if (viewer == null || reviews.isEmpty()) {
			return Set.of();
		}

		List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
		return new LinkedHashSet<>(this.reviewLikeRepository.findLikedReviewIds(viewer.getId(), reviewIds));
	}

	/** 단건 응답(좋아요 수·여부·답장 수 포함). */
	private ReviewResponse toResponse(Review review, boolean editable, User viewer) {
		boolean liked = viewer != null
				&& this.reviewLikeRepository.existsByReviewIdAndUserId(review.getId(), viewer.getId());

		return ReviewResponse.of(review, editable, this.reviewLikeRepository.countByReviewId(review.getId()), liked,
				this.reviewReplyRepository.countByReviewId(review.getId()));
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
