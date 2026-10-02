package com.godsse.backend.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.godsse.backend.api.dto.UpdateProfileRequest;
import com.godsse.backend.api.dto.UserProfileDetailResponse;
import com.godsse.backend.api.dto.UserProfileResponse;
import com.godsse.backend.api.dto.UserSummary;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.domain.User;
import com.godsse.backend.repository.FollowRepository;
import com.godsse.backend.repository.ReviewRepository;
import com.godsse.backend.repository.UserRepository;

/**
 * 사용자 조회·검색·프로필 편집 로직.
 *
 * <p>
 * 클래스에 붙은 {@code @Transactional(readOnly = true)} 는 "이 클래스의 기본은 읽기 전용"이라는 뜻이다.
 * 값을 바꾸는 메서드에만 {@code @Transactional} 을 따로 붙여 쓰기 트랜잭션으로 바꾼다.
 * (읽기 전용은 불필요한 변경 감지를 하지 않아 조금 더 빠르다)
 *
 * <p>
 * 컨트롤러가 넘겨준 {@code loginUser} 는 트랜잭션 밖에서 읽힌 "떨어져 나온" 객체일 수 있으므로,
 * 값을 바꾸기 전에 항상 id 로 다시 조회해 영속 상태의 객체를 사용한다.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserRepository userRepository;
	private final ReviewRepository reviewRepository;
	private final FollowRepository followRepository;
	private final FileStorageService fileStorageService;

	public UserService(UserRepository userRepository, ReviewRepository reviewRepository,
			FollowRepository followRepository, FileStorageService fileStorageService) {
		this.userRepository = userRepository;
		this.reviewRepository = reviewRepository;
		this.followRepository = followRepository;
		this.fileStorageService = fileStorageService;
	}

	public Optional<User> findById(Long id) {
		return this.userRepository.findById(id);
	}

	/** 반드시 있어야 하는 사용자를 id 로 가져온다. 없으면 404. */
	public User getById(Long id) {
		return this.userRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
	}

	public User getByHandle(String handle) {
		return this.userRepository.findByHandle(handle)
			.orElseThrow(() -> new NotFoundException("handle", "해당 핸들의 사용자를 찾을 수 없습니다."));
	}

	public boolean existsByHandle(String handle) {
		return this.userRepository.existsByHandle(handle);
	}

	/**
	 * 프로필 화면용 정보.
	 *
	 * <p>
	 * 감상 수와 팔로워/팔로잉 수까지 함께 담는다. {@code viewer} 는 "지금 보고 있는 사람"이며
	 * 로그인하지 않았다면 null 이다(그때는 {@code isFollowing} 이 false).
	 */
	public UserProfileDetailResponse getProfile(String handle, User viewer) {
		User user = getByHandle(handle);

		long receivedReviewCount = this.reviewRepository.countByRecipientId(user.getId());
		long writtenReviewCount = this.reviewRepository.countByAuthorId(user.getId());
		long followerCount = this.followRepository.countByFollowingId(user.getId());
		long followingCount = this.followRepository.countByFollowerId(user.getId());

		boolean isFollowing = viewer != null
				&& this.followRepository.existsByFollowerIdAndFollowingId(viewer.getId(), user.getId());

		return UserProfileDetailResponse.of(user, receivedReviewCount, writtenReviewCount, followerCount,
				followingCount, isFollowing);
	}

	/**
	 * 닉네임·자기소개를 바꾼다. (핸들과 프로필 이미지는 여기서 바꾸지 않는다)
	 *
	 * <p>
	 * 트랜잭션 안에서 조회한 엔티티는 필드를 바꾸기만 하면 커밋 시점에 자동으로 UPDATE 된다.
	 * (이른바 "변경 감지(dirty checking)" — {@code save()} 를 부를 필요가 없다)
	 */
	@Transactional
	public UserProfileResponse updateProfile(User loginUser, UpdateProfileRequest request) {
		User user = getById(loginUser.getId());

		user.updateProfile(request.nickname().trim(), trimToNull(request.bio()), user.getProfileImageUrl());

		return UserProfileResponse.of(user);
	}

	/**
	 * 프로필 이미지를 교체한다.
	 *
	 * <p>
	 * 순서: 새 파일 저장 → 사용자 정보 갱신 → 이전 파일 삭제.
	 * 새 파일을 먼저 저장하므로, 저장에 실패하면 기존 이미지가 그대로 남는다.
	 * (파일 삭제는 DB 트랜잭션으로 되돌릴 수 없어서 마지막에 한다)
	 */
	@Transactional
	public UserProfileResponse changeProfileImage(User loginUser, MultipartFile file) {
		User user = getById(loginUser.getId());
		String previousUrl = user.getProfileImageUrl();

		String newUrl = this.fileStorageService.storeProfileImage(file);
		user.updateProfile(user.getNickname(), user.getBio(), newUrl);

		this.fileStorageService.deleteProfileImage(previousUrl);

		return UserProfileResponse.of(user);
	}

	/**
	 * 핸들·닉네임으로 사용자를 찾는다.
	 *
	 * <p>
	 * 검색어가 비어 있으면 결과도 비운다. 실수로 전체 사용자 목록을 내려주지 않기 위함이다.
	 */
	public Page<UserSummary> search(String keyword, Pageable pageable) {
		if (keyword == null || keyword.isBlank()) {
			return Page.empty(pageable);
		}

		String trimmed = keyword.trim();
		return this.userRepository
			.findByHandleContainingIgnoreCaseOrNicknameContainingIgnoreCase(trimmed, trimmed, pageable)
			.map(UserSummary::of);
	}

	/** 자기소개는 비어 있으면 null 로 저장해 "없음"과 "빈 문자열"을 구분한다. */
	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
