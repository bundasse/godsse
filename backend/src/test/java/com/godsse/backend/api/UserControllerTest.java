package com.godsse.backend.api;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 조회·검색·편집·이미지 업로드 API 테스트.
 *
 * <p>
 * 업로드 테스트는 실제로 파일을 쓰지만, 테스트 프로파일에서 {@code app.upload.dir} 을
 * {@code ./target/test-uploads} 로 바꾸어 두어 저장소가 더러워지지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("프로필 조회는 200 과 프로필·감상 수를 반환한다")
	void profile() throws Exception {
		signup("godsse");

		this.mockMvc.perform(get("/api/users/godsse"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.handle").value("godsse"))
			.andExpect(jsonPath("$.nickname").value("고드세"))
			.andExpect(jsonPath("$.receivedReviewCount").value(0))
			.andExpect(jsonPath("$.writtenReviewCount").value(0));
	}

	@Test
	@DisplayName("없는 핸들의 프로필을 조회하면 404 와 handle 오류를 반환한다")
	void profileNotFound() throws Exception {
		this.mockMvc.perform(get("/api/users/nobody"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors[0].field").value("handle"));
	}

	@Test
	@DisplayName("유저 검색은 핸들 일부로 찾고 핸들 순으로 정렬한다")
	void search() throws Exception {
		signup("godsse");
		signup("godssefan");

		this.mockMvc.perform(get("/api/users").param("q", "godsse"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(2))
			.andExpect(jsonPath("$.items[0].handle").value("godsse"))
			.andExpect(jsonPath("$.items[1].handle").value("godssefan"));
	}

	@Test
	@DisplayName("검색어가 없으면 빈 목록을 반환한다")
	void searchWithoutKeyword() throws Exception {
		signup("godsse");

		this.mockMvc.perform(get("/api/users"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(0))
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("프로필 편집은 닉네임·자기소개를 바꾸고 앞뒤 공백을 제거한다")
	void updateProfile() throws Exception {
		MockHttpSession session = signup("godsse");

		this.mockMvc
			.perform(patch("/api/users/me").session(session).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nickname":"새이름","bio":"  자기소개입니다  "}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nickname").value("새이름"))
			.andExpect(jsonPath("$.bio").value("자기소개입니다"));
	}

	@Test
	@DisplayName("로그인하지 않고 프로필을 편집하면 401 을 반환한다")
	void updateProfileWithoutLogin() throws Exception {
		this.mockMvc
			.perform(patch("/api/users/me").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nickname":"새이름","bio":""}
						"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("닉네임이 공백뿐이면 400 과 nickname 오류를 반환한다")
	void updateProfileWithBlankNickname() throws Exception {
		MockHttpSession session = signup("godsse");

		this.mockMvc
			.perform(patch("/api/users/me").session(session).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nickname":"   ","bio":""}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("nickname"));
	}

	@Test
	@DisplayName("프로필 이미지를 올리면 /uploads/profiles/ 경로가 저장된다")
	void uploadAvatar() throws Exception {
		MockHttpSession session = signup("godsse");

		MockMultipartFile image = new MockMultipartFile("file", "profile.PNG", MediaType.IMAGE_PNG_VALUE,
				"fake-png-bytes".getBytes(StandardCharsets.UTF_8));

		this.mockMvc.perform(multipart("/api/users/me/avatar").file(image).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profileImageUrl").value(startsWith("/uploads/profiles/")));
	}

	@Test
	@DisplayName("이미지가 아닌 파일을 올리면 400 과 file 오류를 반환한다")
	void uploadAvatarWithTextFile() throws Exception {
		MockHttpSession session = signup("godsse");

		MockMultipartFile text = new MockMultipartFile("file", "note.txt", MediaType.TEXT_PLAIN_VALUE,
				"hello".getBytes(StandardCharsets.UTF_8));

		this.mockMvc.perform(multipart("/api/users/me/avatar").file(text).session(session))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("file"));
	}

	@Test
	@DisplayName("로그인하지 않고 프로필 이미지를 올리면 401 을 반환한다")
	void uploadAvatarWithoutLogin() throws Exception {
		MockMultipartFile image = new MockMultipartFile("file", "profile.png", MediaType.IMAGE_PNG_VALUE,
				"fake-png-bytes".getBytes(StandardCharsets.UTF_8));

		this.mockMvc.perform(multipart("/api/users/me/avatar").file(image)).andExpect(status().isUnauthorized());
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
