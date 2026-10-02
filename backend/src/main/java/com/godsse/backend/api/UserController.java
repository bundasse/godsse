package com.godsse.backend.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.godsse.backend.api.dto.PageResponse;
import com.godsse.backend.api.dto.UpdateProfileRequest;
import com.godsse.backend.api.dto.UserProfileDetailResponse;
import com.godsse.backend.api.dto.UserProfileResponse;
import com.godsse.backend.api.dto.UserSummary;
import com.godsse.backend.common.LoginUser;
import com.godsse.backend.domain.User;
import com.godsse.backend.service.UserService;

import jakarta.validation.Valid;

/**
 * 사용자 검색 / 프로필 조회 / 프로필 편집 / 프로필 이미지 업로드.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

	/** 한 번에 내려줄 수 있는 최대 개수. 요청이 크게 보내도 서버에서 제한한다. */
	private static final int MAX_PAGE_SIZE = 50;

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 유저 검색. 로그인하지 않아도 사용할 수 있다.
	 *
	 * @param keyword 핸들 또는 닉네임의 일부(비어 있으면 빈 목록)
	 */
	@GetMapping
	public PageResponse<UserSummary> search(@RequestParam(name = "q", required = false) String keyword,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {

		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.ASC, "handle"));

		return PageResponse.of(this.userService.search(keyword, pageable));
	}

	/**
	 * 프로필 조회. 로그인하지 않아도 볼 수 있다.
	 *
	 * <p>
	 * 로그인한 상태라면 {@code isFollowing}(내가 이 사람을 팔로우 중인지)도 함께 내려간다.
	 */
	@GetMapping("/{handle}")
	public UserProfileDetailResponse profile(@PathVariable String handle,
			@LoginUser(required = false) User viewer) {
		return this.userService.getProfile(handle, viewer);
	}

	/**
	 * 내 프로필 편집(닉네임·자기소개).
	 */
	@PatchMapping("/me")
	public UserProfileResponse updateMe(@LoginUser User user, @Valid @RequestBody UpdateProfileRequest request) {
		return this.userService.updateProfile(user, request);
	}

	/**
	 * 내 프로필 이미지 업로드(multipart/form-data, 필드 이름 {@code file}).
	 *
	 * <p>
	 * 파일 전송은 JSON 이 아니라서 별도 API 로 두었다.
	 */
	@PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserProfileResponse uploadAvatar(@LoginUser User user, @RequestPart("file") MultipartFile file) {
		return this.userService.changeProfileImage(user, file);
	}

}
