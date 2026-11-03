package com.godsse.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * 히트맵 룰 색 API 테스트.
 *
 * <p>
 * 색은 <b>사람마다 따로</b> 저장되므로, 남의 색이 내 목록에 섞이지 않는지도 확인한다.
 * 룰 이름을 경로가 아니라 본문·쿼리로 받는 이유(자유 입력에 {@code /} 가 들어갈 수 있음)는
 * {@link RuleColorController} 주석 참고.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RuleColorControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("색을 저장하면 소문자로 돌려주고 내 목록에 들어간다")
	void saveAndList() throws Exception {
		MockHttpSession session = signup("colorfan");

		this.mockMvc
			.perform(put("/api/users/me/rule-colors").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(colorJson("던전월드", "#16A34A")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ruleName").value("던전월드"))
			.andExpect(jsonPath("$.color").value("#16a34a"));

		this.mockMvc.perform(get("/api/users/me/rule-colors").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].ruleName").value("던전월드"))
			.andExpect(jsonPath("$[0].color").value("#16a34a"));
	}

	@Test
	@DisplayName("같은 룰에 다시 저장하면 줄이 늘지 않고 색만 바뀐다")
	void saveTwiceUpdatesSameRow() throws Exception {
		MockHttpSession session = signup("colorfan");

		saveColor(session, "던전월드", "#16a34a");
		saveColor(session, "던전월드", "#ff8800");

		this.mockMvc.perform(get("/api/users/me/rule-colors").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].color").value("#ff8800"));
	}

	@Test
	@DisplayName("앞뒤 공백이 있는 룰 이름은 다듬어 저장한다")
	void trimsRuleName() throws Exception {
		MockHttpSession session = signup("colorfan");

		this.mockMvc
			.perform(put("/api/users/me/rule-colors").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(colorJson("  던전월드  ", "#16a34a")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ruleName").value("던전월드"));

		// 같은 룰로 다시 저장해도 한 줄이어야 한다(공백 때문에 다른 룰로 저장되면 두 줄이 된다).
		saveColor(session, "던전월드", "#ff8800");

		this.mockMvc.perform(get("/api/users/me/rule-colors").session(session))
			.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	@DisplayName("색을 지우면 목록에서 빠지고, 같은 요청을 또 보내도 성공한다")
	void reset() throws Exception {
		MockHttpSession session = signup("colorfan");
		saveColor(session, "던전월드", "#ff8800");

		this.mockMvc
			.perform(delete("/api/users/me/rule-colors").session(session).param("ruleName", "던전월드"))
			.andExpect(status().isNoContent());

		// 이미 지운 색을 다시 지워도 404 가 아니라 성공이다(같은 요청 = 같은 결과).
		this.mockMvc
			.perform(delete("/api/users/me/rule-colors").session(session).param("ruleName", "던전월드"))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/users/me/rule-colors").session(session))
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("색은 사람마다 따로 저장된다")
	void colorsArePerUser() throws Exception {
		MockHttpSession mine = signup("colorfan");
		MockHttpSession other = signup("otherfan");

		saveColor(mine, "던전월드", "#ff8800");

		this.mockMvc.perform(get("/api/users/me/rule-colors").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("색 형식이 틀리면 400 이고 color 필드를 알려준다")
	void rejectsInvalidColor() throws Exception {
		MockHttpSession session = signup("colorfan");

		this.mockMvc
			.perform(put("/api/users/me/rule-colors").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(colorJson("던전월드", "ff8800")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("color"));
	}

	@Test
	@DisplayName("룰 이름이 비면 400 이고 ruleName 필드를 알려준다")
	void rejectsBlankRuleName() throws Exception {
		MockHttpSession session = signup("colorfan");

		this.mockMvc
			.perform(put("/api/users/me/rule-colors").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(colorJson("   ", "#ff8800")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("ruleName"));
	}

	@Test
	@DisplayName("색 조회·저장·삭제 모두 로그인이 필요하다")
	void requiresLogin() throws Exception {
		this.mockMvc.perform(get("/api/users/me/rule-colors")).andExpect(status().isUnauthorized());

		this.mockMvc
			.perform(put("/api/users/me/rule-colors").contentType(MediaType.APPLICATION_JSON)
				.content(colorJson("던전월드", "#ff8800")))
			.andExpect(status().isUnauthorized());

		this.mockMvc.perform(delete("/api/users/me/rule-colors").param("ruleName", "던전월드"))
			.andExpect(status().isUnauthorized());
	}

	private void saveColor(MockHttpSession session, String ruleName, String color) throws Exception {
		this.mockMvc
			.perform(put("/api/users/me/rule-colors").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(colorJson(ruleName, color)))
			.andExpect(status().isOk());
	}

	private static String colorJson(String ruleName, String color) {
		return "{\"ruleName\":\"" + ruleName + "\",\"color\":\"" + color + "\"}";
	}

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
