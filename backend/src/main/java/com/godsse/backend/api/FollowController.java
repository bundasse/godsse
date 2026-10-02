package com.godsse.backend.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.godsse.backend.api.dto.PageResponse;
import com.godsse.backend.api.dto.UserSummary;
import com.godsse.backend.common.LoginUser;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.FollowService;

/**
 * 팔로우 / 언팔로우 / 팔로워·팔로잉 목록.
 *
 * <p>
 * 모두 {@code /api/users/{handle}} 아래에 붙는다. 경로가 겹치지 않으므로
 * {@link UserController} 와 클래스를 나눠 두어도 Spring 이 알아서 알맞은 메서드를 고른다.
 */
@RestController
@RequestMapping("/api/users/{handle}")
public class FollowController {

	private static final int MAX_PAGE_SIZE = 50;

	private final FollowService followService;

	public FollowController(FollowService followService) {
		this.followService = followService;
	}

	/**
	 * 팔로우한다. (204, 본문 없음)
	 */
	@PostMapping("/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void follow(@LoginUser User user, @PathVariable String handle) {
		this.followService.follow(user, handle);
	}

	/**
	 * 언팔로우한다. (204, 본문 없음)
	 */
	@DeleteMapping("/follow")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unfollow(@LoginUser User user, @PathVariable String handle) {
		this.followService.unfollow(user, handle);
	}

	/**
	 * 팔로워 목록(이 사람을 팔로우하는 사람들). 로그인 없이 볼 수 있다.
	 */
	@GetMapping("/followers")
	public PageResponse<UserSummary> followers(@PathVariable String handle,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {

		return PageResponse.of(this.followService.followers(handle, toPageable(page, size)));
	}

	/**
	 * 팔로잉 목록(이 사람이 팔로우하는 사람들). 로그인 없이 볼 수 있다.
	 */
	@GetMapping("/following")
	public PageResponse<UserSummary> following(@PathVariable String handle,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {

		return PageResponse.of(this.followService.following(handle, toPageable(page, size)));
	}

	/**
	 * 정렬은 저장소의 JPQL 안에 적혀 있으므로 여기서는 페이지 정보만 만든다.
	 */
	private static Pageable toPageable(int page, int size) {
		return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
	}

}
