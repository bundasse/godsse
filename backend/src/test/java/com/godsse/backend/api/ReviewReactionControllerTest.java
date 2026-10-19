package com.godsse.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * 좋아요·답장 API 테스트.
 *
 * <p>
 * 좋아요와 답장도 <b>감상의 공개 범위</b>를 따른다. 볼 수 없는 감상에는 누를 수도, 답장할 수도 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewReactionControllerTest {

	private static final String LONG_BODY = "가".repeat(120);

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("좋아요를 누르면 감상의 좋아요 수와 내 좋아요 여부가 바뀐다")
	void like() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession liker = signup("liker");
		signup("partner");
		long id = createReview(writer, "partner", null);

		this.mockMvc.perform(post("/api/reviews/" + id + "/like").session(liker)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/reviews/" + id).session(liker))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likeCount").value(1))
			.andExpect(jsonPath("$.liked").value(true));

		// 비로그인은 좋아요 수만 보이고, 내 좋아요 여부는 false
		this.mockMvc.perform(get("/api/reviews/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likeCount").value(1))
			.andExpect(jsonPath("$.liked").value(false));
	}

	@Test
	@DisplayName("같은 감상에 두 번 좋아요하면 409 를 반환한다")
	void duplicateLike() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession liker = signup("liker");
		signup("partner");
		long id = createReview(writer, "partner", null);

		this.mockMvc.perform(post("/api/reviews/" + id + "/like").session(liker)).andExpect(status().isNoContent());

		this.mockMvc.perform(post("/api/reviews/" + id + "/like").session(liker))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value("이미 좋아요한 감상입니다."));
	}

	@Test
	@DisplayName("좋아요를 취소하면 수가 0 으로 돌아가고, 다시 취소하면 404 다")
	void unlike() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession liker = signup("liker");
		signup("partner");
		long id = createReview(writer, "partner", null);

		this.mockMvc.perform(post("/api/reviews/" + id + "/like").session(liker)).andExpect(status().isNoContent());
		this.mockMvc.perform(delete("/api/reviews/" + id + "/like").session(liker)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/reviews/" + id).session(liker))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.likeCount").value(0))
			.andExpect(jsonPath("$.liked").value(false));

		this.mockMvc.perform(delete("/api/reviews/" + id + "/like").session(liker))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("볼 수 없는 감상에는 좋아요할 수 없고, 비로그인은 401 이다")
	void likeRequiresVisibilityAndLogin() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");
		MockHttpSession stranger = signup("stranger");
		long id = createReview(writer, "partner", "PRIVATE");

		this.mockMvc.perform(post("/api/reviews/" + id + "/like").session(stranger))
			.andExpect(status().isForbidden());

		this.mockMvc.perform(post("/api/reviews/" + id + "/like")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("답장을 남기면 목록과 감상의 답장 수에 반영된다")
	void addReply() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		long id = createReview(writer, "partner", null);

		addReply(partner, id, "저도 정말 즐거웠어요!");

		this.mockMvc.perform(get("/api/reviews/" + id + "/replies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].body").value("저도 정말 즐거웠어요!"))
			.andExpect(jsonPath("$[0].author.handle").value("partner"))
			.andExpect(jsonPath("$[0].editable").value(false));

		this.mockMvc.perform(get("/api/reviews/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.replyCount").value(1));
	}

	@Test
	@DisplayName("답장 내용이 비면 400 을 반환한다")
	void addBlankReply() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		long id = createReview(writer, "partner", null);

		this.mockMvc
			.perform(post("/api/reviews/" + id + "/replies").session(partner)
				.contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("body"));
	}

	@Test
	@DisplayName("답장은 작성자만 삭제할 수 있다")
	void deleteReply() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		MockHttpSession other = signup("other");
		long id = createReview(writer, "partner", null);
		long replyId = addReply(partner, id, "좋았습니다");

		this.mockMvc.perform(delete("/api/reviews/" + id + "/replies/" + replyId).session(other))
			.andExpect(status().isForbidden());

		this.mockMvc.perform(delete("/api/reviews/" + id + "/replies/" + replyId).session(partner))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/reviews/" + id + "/replies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("볼 수 없는 감상의 답장은 조회·작성 모두 403 이다")
	void replyRequiresVisibility() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");
		MockHttpSession stranger = signup("stranger");
		long id = createReview(writer, "partner", "PRIVATE");

		this.mockMvc.perform(get("/api/reviews/" + id + "/replies").session(stranger))
			.andExpect(status().isForbidden());

		this.mockMvc
			.perform(post("/api/reviews/" + id + "/replies").session(stranger)
				.contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"몰래 답장\"}"))
			.andExpect(status().isForbidden());
	}

	/** writer 가 toHandle 에게 감상을 남기고 id 를 돌려준다. visibility 가 null 이면 기본(공개). */
	private long createReview(MockHttpSession writer, String toHandle, String visibility) throws Exception {
		StringBuilder json = new StringBuilder("{\"toHandle\":\"").append(toHandle)
			.append("\",\"playedOn\":\"2026-10-01\",\"ruleName\":\"던전월드\",\"body\":\"").append(LONG_BODY)
			.append("\"");
		if (visibility != null) {
			json.append(",\"visibility\":\"").append(visibility).append("\"");
		}
		json.append("}");

		MvcResult result = this.mockMvc
			.perform(post("/api/reviews").session(writer).contentType(MediaType.APPLICATION_JSON).content(json.toString()))
			.andExpect(status().isCreated())
			.andReturn();

		return idOf(result);
	}

	private long addReply(MockHttpSession session, long reviewId, String body) throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/reviews/" + reviewId + "/replies").session(session)
				.contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + body + "\"}"))
			.andExpect(status().isCreated())
			.andReturn();

		return idOf(result);
	}

	private static long idOf(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
		if (!matcher.find()) {
			throw new IllegalStateException("응답에서 id 를 찾지 못했습니다: " + body);
		}
		return Long.parseLong(matcher.group(1));
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
