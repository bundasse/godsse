package com.godsse.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * 감상(리뷰) 작성·조회·수정·삭제 API 테스트.
 *
 * <p>
 * 공개 범위(PUBLIC / FOLLOWERS / PRIVATE)에 따라 보이거나 가려지는지를 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewControllerTest {

	/** 100자 이상이어야 하므로 충분히 긴 본문을 만든다. */
	private static final String LONG_BODY = "가".repeat(120);

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("감상을 작성하면 201 과 저장된 내용을 반환한다")
	void create() throws Exception {
		MockHttpSession session = signup("writer");
		signup("partner");

		this.mockMvc
			.perform(post("/api/reviews").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson("partner", LONG_BODY, null, null)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.author.handle").value("writer"))
			.andExpect(jsonPath("$.recipient.handle").value("partner"))
			.andExpect(jsonPath("$.playedOn").value("2026-09-20"))
			.andExpect(jsonPath("$.ruleName").value("크툴루의 부름 7판"))
			.andExpect(jsonPath("$.scenarioName").value("밤의 미궁"))
			.andExpect(jsonPath("$.body").value(LONG_BODY))
			.andExpect(jsonPath("$.spoilerMode").value("NONE"))
			.andExpect(jsonPath("$.visibility").value("PUBLIC"))
			.andExpect(jsonPath("$.editable").value(true))
			.andExpect(jsonPath("$.tags.length()").value(2));
	}

	@Test
	@DisplayName("감상이 100자 미만이면 400 과 body 오류를 반환한다")
	void createWithShortBody() throws Exception {
		MockHttpSession session = signup("writer");
		signup("partner");

		this.mockMvc
			.perform(post("/api/reviews").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson("partner", "짧은 감상", null, null)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("body"));
	}

	@Test
	@DisplayName("플레이 날짜가 없으면 400 과 playedOn 오류를 반환한다")
	void createWithoutPlayedOn() throws Exception {
		MockHttpSession session = signup("writer");
		signup("partner");

		this.mockMvc
			.perform(post("/api/reviews").session(session).contentType(MediaType.APPLICATION_JSON)
				.content("{\"toHandle\":\"partner\",\"ruleName\":\"크툴루의 부름 7판\",\"body\":\"" + LONG_BODY + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("playedOn"));
	}

	@Test
	@DisplayName("자신에게 감상을 남기면 400 을 반환한다")
	void createToSelf() throws Exception {
		MockHttpSession session = signup("writer");

		this.mockMvc
			.perform(post("/api/reviews").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson("writer", LONG_BODY, null, null)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("toHandle"));
	}

	@Test
	@DisplayName("없는 사람에게 감상을 남기면 404 를 반환한다")
	void createToUnknownHandle() throws Exception {
		MockHttpSession session = signup("writer");

		this.mockMvc
			.perform(post("/api/reviews").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson("nobody", LONG_BODY, null, null)))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("로그인하지 않고 감상을 쓰면 401 을 반환한다")
	void createWithoutLogin() throws Exception {
		signup("partner");

		this.mockMvc
			.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson("partner", LONG_BODY, null, null)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("공개 감상은 로그인하지 않아도 볼 수 있다")
	void getPublicReview() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");

		long id = createReview(writer, "partner", LONG_BODY, null, null);

		this.mockMvc.perform(get("/api/reviews/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.body").value(LONG_BODY))
			.andExpect(jsonPath("$.editable").value(false));
	}

	@Test
	@DisplayName("없는 감상을 조회하면 404 를 반환한다")
	void getUnknownReview() throws Exception {
		this.mockMvc.perform(get("/api/reviews/999999")).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("PRIVATE 감상은 작성자와 받은 사람만 볼 수 있다")
	void privateReviewVisibility() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		MockHttpSession other = signup("other");

		long id = createReview(writer, "partner", LONG_BODY, null, "PRIVATE");

		// 비로그인 · 제3자
		this.mockMvc.perform(get("/api/reviews/" + id)).andExpect(status().isForbidden());
		this.mockMvc.perform(get("/api/reviews/" + id).session(other)).andExpect(status().isForbidden());

		// 작성자는 수정 가능(editable=true), 받은 사람은 읽기만(editable=false)
		this.mockMvc.perform(get("/api/reviews/" + id).session(writer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.editable").value(true));
		this.mockMvc.perform(get("/api/reviews/" + id).session(partner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.editable").value(false));
	}

	@Test
	@DisplayName("FOLLOWERS 감상은 작성자를 팔로우한 사람만 볼 수 있다")
	void followersVisibility() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");
		MockHttpSession fan = signup("fan");
		MockHttpSession stranger = signup("stranger");

		long id = createReview(writer, "partner", LONG_BODY, null, "FOLLOWERS");

		this.mockMvc.perform(get("/api/reviews/" + id).session(stranger)).andExpect(status().isForbidden());

		this.mockMvc.perform(post("/api/users/writer/follow").session(fan)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/reviews/" + id).session(fan)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("받은 감상 목록은 본문 전체 대신 미리보기만 내려준다")
	void listByRecipient() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");

		String longBody = "나".repeat(300);
		createReview(writer, "partner", longBody, null, null);

		MvcResult result = this.mockMvc.perform(get("/api/reviews").param("recipient", "partner"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(1))
			.andExpect(jsonPath("$.items[0].author.handle").value("writer"))
			.andExpect(jsonPath("$.items[0].recipient.handle").value("partner"))
			.andExpect(jsonPath("$.items[0].bodyPreview").isNotEmpty())
			.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		assertThat(responseBody).contains("…");
		assertThat(responseBody).doesNotContain(longBody);
	}

	@Test
	@DisplayName("PRIVATE 감상은 남의 목록에 나오지 않는다")
	void privateReviewHiddenFromOthersList() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		MockHttpSession other = signup("other");

		createReview(writer, "partner", LONG_BODY, null, "PRIVATE");

		this.mockMvc.perform(get("/api/reviews").param("recipient", "partner").session(other))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(0));

		this.mockMvc.perform(get("/api/reviews").param("recipient", "partner").session(partner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.total").value(1));
	}

	@Test
	@DisplayName("감상은 작성자만 수정할 수 있다")
	void updateReview() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");
		MockHttpSession other = signup("other");

		long id = createReview(writer, "partner", LONG_BODY, null, null);
		String updateJson = "{\"playedOn\":\"2026-09-21\",\"ruleName\":\"던전월드\",\"body\":\"" + "다".repeat(120)
				+ "\"}";

		this.mockMvc
			.perform(patch("/api/reviews/" + id).session(other).contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
			.andExpect(status().isForbidden());

		this.mockMvc
			.perform(patch("/api/reviews/" + id).session(writer).contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ruleName").value("던전월드"))
			.andExpect(jsonPath("$.playedOn").value("2026-09-21"));
	}

	@Test
	@DisplayName("감상은 작성자만 삭제할 수 있다")
	void deleteReview() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");
		MockHttpSession other = signup("other");

		long id = createReview(writer, "partner", LONG_BODY, null, null);

		this.mockMvc.perform(delete("/api/reviews/" + id).session(other)).andExpect(status().isForbidden());

		this.mockMvc.perform(delete("/api/reviews/" + id).session(writer)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/reviews/" + id)).andExpect(status().isNotFound());
	}

	/** 감상을 만들고 id 를 돌려준다. */
	private long createReview(MockHttpSession session, String toHandle, String body, String spoilerMode,
			String visibility) throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/reviews").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson(toHandle, body, spoilerMode, visibility)))
			.andExpect(status().isCreated())
			.andReturn();

		return idOf(result);
	}

	private static String reviewJson(String toHandle, String body, String spoilerMode, String visibility) {
		StringBuilder json = new StringBuilder();
		json.append("{\"toHandle\":\"").append(toHandle).append("\",\"playedOn\":\"2026-09-20\",");
		json.append("\"ruleName\":\"크툴루의 부름 7판\",\"scenarioName\":\"밤의 미궁\",");
		json.append("\"body\":\"").append(body).append("\"");
		if (spoilerMode != null) {
			json.append(",\"spoilerMode\":\"").append(spoilerMode).append("\"");
		}
		if (visibility != null) {
			json.append(",\"visibility\":\"").append(visibility).append("\"");
		}
		json.append(",\"tags\":[\"RP\",\"HORROR\"]}");
		return json.toString();
	}

	/** 응답 JSON 에서 id 만 뽑아낸다. (Jackson 버전에 의존하지 않도록 정규식 사용) */
	private static long idOf(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
		if (!matcher.find()) {
			throw new IllegalStateException("응답에서 id 를 찾지 못했습니다: " + body);
		}
		return Long.parseLong(matcher.group(1));
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
