package com.godsse.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팔로우·언팔로우·팔로워/팔로잉 목록 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FollowControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("팔로우하면 204 를 반환하고 팔로워 수와 팔로우 여부가 바뀐다")
	void follow() throws Exception {
		MockHttpSession session = signup("follower");
		signup("target");

		this.mockMvc.perform(post("/api/users/target/follow").session(session)).andExpect(status().isNoContent());

		// 팔로우한 사람이 보는 프로필
		this.mockMvc.perform(get("/api/users/target").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followerCount").value(1))
			.andExpect(jsonPath("$.isFollowing").value(true));

		// 로그인하지 않고 보는 프로필 (팔로우 여부는 false)
		this.mockMvc.perform(get("/api/users/target"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followerCount").value(1))
			.andExpect(jsonPath("$.isFollowing").value(false));

		// 팔로우한 사람의 팔로잉 수도 늘어난다
		this.mockMvc.perform(get("/api/users/follower"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followingCount").value(1));
	}

	@Test
	@DisplayName("로그인하지 않고 팔로우하면 401 을 반환한다")
	void followWithoutLogin() throws Exception {
		signup("target");

		this.mockMvc.perform(post("/api/users/target/follow")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("이미 팔로우한 사용자를 다시 팔로우하면 409 를 반환한다")
	void duplicateFollow() throws Exception {
		MockHttpSession session = signup("follower");
		signup("target");

		this.mockMvc.perform(post("/api/users/target/follow").session(session)).andExpect(status().isNoContent());

		this.mockMvc.perform(post("/api/users/target/follow").session(session))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 팔로우한 사용자입니다."));
	}

	@Test
	@DisplayName("자기 자신을 팔로우하면 400 을 반환한다")
	void followSelf() throws Exception {
		MockHttpSession session = signup("godsse");

		this.mockMvc.perform(post("/api/users/godsse/follow").session(session))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("자기 자신은 팔로우할 수 없습니다."));
	}

	@Test
	@DisplayName("없는 핸들을 팔로우하면 404 를 반환한다")
	void followUnknownHandle() throws Exception {
		MockHttpSession session = signup("godsse");

		this.mockMvc.perform(post("/api/users/nobody/follow").session(session))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors[0].field").value("handle"));
	}

	@Test
	@DisplayName("언팔로우하면 204 를 반환하고 팔로우 여부가 false 로 돌아온다")
	void unfollow() throws Exception {
		MockHttpSession session = signup("follower");
		signup("target");

		this.mockMvc.perform(post("/api/users/target/follow").session(session)).andExpect(status().isNoContent());

		this.mockMvc.perform(delete("/api/users/target/follow").session(session)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/users/target").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.followerCount").value(0))
			.andExpect(jsonPath("$.isFollowing").value(false));
	}

	@Test
	@DisplayName("팔로우하지 않은 사용자를 언팔로우하면 404 를 반환한다")
	void unfollowWithoutFollow() throws Exception {
		MockHttpSession session = signup("follower");
		signup("target");

		this.mockMvc.perform(delete("/api/users/target/follow").session(session))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("팔로우하지 않은 사용자입니다."));
	}

	@Test
	@DisplayName("팔로워 목록에는 나를 팔로우한 사람이 나온다")
	void followers() throws Exception {
		MockHttpSession session = signup("follower");
		signup("target");

		this.mockMvc.perform(post("/api/users/target/follow").session(session)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/users/target/followers"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(1))
			.andExpect(jsonPath("$.items[0].handle").value("follower"))
			.andExpect(jsonPath("$.items[0].nickname").value("고드세"));
	}

	@Test
	@DisplayName("팔로잉 목록에는 내가 팔로우한 사람이 나온다")
	void following() throws Exception {
		MockHttpSession session = signup("follower");
		signup("target");

		this.mockMvc.perform(post("/api/users/target/follow").session(session)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/users/follower/following"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(1))
			.andExpect(jsonPath("$.items[0].handle").value("target"));
	}

	/** 회원가입해서 세션을 돌려준다. (회원가입 시 자동 로그인되므로 세션이 생긴다) */
	private MockHttpSession signup(String handle) throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("{\"handle\":\"" + handle + "\",\"email\":\"" + handle
						+ "@example.com\",\"password\":\"password123\",\"nickname\":\"고드세\"}"))
			.andExpect(status().isCreated())
			.andReturn();

		return (MockHttpSession) result.getRequest().getSession(false);
	}

}
