package com.godsse.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
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
 * 회원가입·로그인·로그아웃·내 정보 API 테스트.
 *
 * <p>
 * 로그인 상태는 세션에 있으므로, 회원가입/로그인 응답에서 세션을 꺼내 다음 요청에 넘겨 준다.
 * ({@code .session(session)})
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

	private static final String SIGNUP_BODY = """
			{"handle":"GODSSE","email":"GODSSE@example.com","password":"password123","nickname":"고드세"}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("회원가입에 성공하면 201 과 프로필을 반환하고, 바로 로그인 상태가 된다")
	void signup() throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
			.andExpect(status().isCreated())
			// 핸들과 이메일은 소문자로 통일되어 저장된다
			.andExpect(jsonPath("$.handle").value("godsse"))
			.andExpect(jsonPath("$.nickname").value("고드세"))
			.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		this.mockMvc.perform(get("/api/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.handle").value("godsse"))
			.andExpect(jsonPath("$.email").doesNotExist());
	}

	@Test
	@DisplayName("핸들이 중복이면 409 와 handle 필드 오류를 반환한다")
	void signupWithDuplicatedHandle() throws Exception {
		this.mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
			.andExpect(status().isCreated());

		this.mockMvc
			.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"handle":"godsse","email":"other@example.com","password":"password123","nickname":"다른사람"}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 사용 중인 핸들입니다."))
			.andExpect(jsonPath("$.errors[0].field").value("handle"));
	}

	@Test
	@DisplayName("이메일이 중복이면 409 와 email 필드 오류를 반환한다")
	void signupWithDuplicatedEmail() throws Exception {
		this.mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
			.andExpect(status().isCreated());

		this.mockMvc
			.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"handle":"another","email":"godsse@example.com","password":"password123","nickname":"다른사람"}
						"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors[0].field").value("email"));
	}

	@Test
	@DisplayName("입력값이 규칙에 맞지 않으면 400 과 필드 오류를 반환한다")
	void signupWithInvalidInput() throws Exception {
		this.mockMvc
			.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"handle":"a b","email":"not-an-email","password":"123","nickname":""}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.length()").value(4));
	}

	@Test
	@DisplayName("로그인에 성공하면 200 과 프로필을 반환한다")
	void login() throws Exception {
		this.mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
			.andExpect(status().isCreated());

		this.mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"handle":"GODSSE","password":"password123"}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.handle").value("godsse"));
	}

	@Test
	@DisplayName("비밀번호가 틀리면 401 을 반환한다")
	void loginWithWrongPassword() throws Exception {
		this.mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
			.andExpect(status().isCreated());

		this.mockMvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"handle":"godsse","password":"wrong-password"}
						"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("핸들 또는 비밀번호가 올바르지 않습니다."));
	}

	@Test
	@DisplayName("로그인하지 않고 /api/auth/me 를 호출하면 401 을 반환한다")
	void meWithoutLogin() throws Exception {
		this.mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
	}

	@Test
	@DisplayName("로그아웃하면 세션이 무효화된다")
	void logout() throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP_BODY))
			.andExpect(status().isCreated())
			.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		this.mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());

		assertThat(session.isInvalid()).isTrue();
	}

}
