package com.godsse.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.api.dto.UserSummary;
import com.godsse.backend.common.exception.BadRequestException;
import com.godsse.backend.common.exception.ConflictException;
import com.godsse.backend.common.exception.NotFoundException;
import com.godsse.backend.domain.Follow;
import com.godsse.backend.domain.User;
import com.godsse.backend.repository.FollowRepository;
import com.godsse.backend.repository.UserRepository;

/**
 * 팔로우 / 언팔로우 / 팔로워·팔로잉 목록.
 *
 * <p>
 * 팔로우는 "누가(follower) 누구를(following) 팔로우한다"는 관계 한 줄이다.
 * 같은 조합이 두 번 저장되지 않도록 확인하고, 자기 자신은 팔로우할 수 없게 막는다.
 */
@Service
@Transactional(readOnly = true)
public class FollowService {

	private final FollowRepository followRepository;
	private final UserRepository userRepository;

	public FollowService(FollowRepository followRepository, UserRepository userRepository) {
		this.followRepository = followRepository;
		this.userRepository = userRepository;
	}

	/**
	 * 팔로우한다.
	 *
	 * @throws BadRequestException 자기 자신을 팔로우하려 할 때 (400)
	 * @throws ConflictException   이미 팔로우한 사용자일 때 (409)
	 * @throws NotFoundException   대상 핸들이 없을 때 (404)
	 */
	@Transactional
	public void follow(User loginUser, String targetHandle) {
		// 컨트롤러가 넘긴 loginUser 는 트랜잭션 밖의 객체일 수 있으므로 id 로 다시 읽는다.
		User me = getUserById(loginUser.getId());
		User target = getUserByHandle(targetHandle);

		if (me.getId().equals(target.getId())) {
			throw new BadRequestException("handle", "자기 자신은 팔로우할 수 없습니다.");
		}
		if (this.followRepository.existsByFollowerIdAndFollowingId(me.getId(), target.getId())) {
			throw new ConflictException("handle", "이미 팔로우한 사용자입니다.");
		}

		this.followRepository.save(new Follow(me, target));
	}

	/**
	 * 언팔로우한다.
	 *
	 * @throws NotFoundException 팔로우하고 있지 않을 때 (404)
	 */
	@Transactional
	public void unfollow(User loginUser, String targetHandle) {
		User target = getUserByHandle(targetHandle);

		Follow follow = this.followRepository
			.findByFollowerIdAndFollowingId(loginUser.getId(), target.getId())
			.orElseThrow(() -> new NotFoundException("handle", "팔로우하지 않은 사용자입니다."));

		this.followRepository.delete(follow);
	}

	/** 이 사람을 팔로우하는 사람 목록(팔로워). */
	public Page<UserSummary> followers(String handle, Pageable pageable) {
		User user = getUserByHandle(handle);
		return this.followRepository.findFollowerUsers(user.getId(), pageable).map(UserSummary::of);
	}

	/** 이 사람이 팔로우하는 사람 목록(팔로잉). */
	public Page<UserSummary> following(String handle, Pageable pageable) {
		User user = getUserByHandle(handle);
		return this.followRepository.findFollowingUsers(user.getId(), pageable).map(UserSummary::of);
	}

	private User getUserById(Long id) {
		return this.userRepository.findById(id)
			.orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
	}

	private User getUserByHandle(String handle) {
		return this.userRepository.findByHandle(handle)
			.orElseThrow(() -> new NotFoundException("handle", "해당 핸들의 사용자를 찾을 수 없습니다."));
	}

}
