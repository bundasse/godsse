package com.godsse.backend.api;

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
 * 세션 일정(달력) API 테스트.
 *
 * <p>
 * 일정은 나만 보는 계획표이므로, 남의 일정에 접근하면 403 이 되는지도 함께 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SessionPlanControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("일정을 만들면 201 과 내용을 반환하고 상태는 PLANNED 다")
	void create() throws Exception {
		MockHttpSession session = signup("planner");

		this.mockMvc
			.perform(post("/api/sessions").session(session).contentType(MediaType.APPLICATION_JSON).content(planJson(
					"정기 캠페인 3화", "2026-10-20", "크툴루의 부름 7판", "밤의 미궁", "false", "모집 중")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.title").value("정기 캠페인 3화"))
			.andExpect(jsonPath("$.plannedDate").value("2026-10-20"))
			.andExpect(jsonPath("$.ruleName").value("크툴루의 부름 7판"))
			.andExpect(jsonPath("$.scenarioName").value("밤의 미궁"))
			.andExpect(jsonPath("$.memo").value("모집 중"))
			.andExpect(jsonPath("$.gm").value(false))
			.andExpect(jsonPath("$.status").value("PLANNED"));
	}

	@Test
	@DisplayName("기간 내 일정만 날짜 순으로 돌려준다")
	void listByMonth() throws Exception {
		MockHttpSession session = signup("planner");

		createPlan(session, "10월 일정", "2026-10-20");
		createPlan(session, "9월 일정", "2026-09-30");
		createPlan(session, "11월 일정", "2026-11-01");

		this.mockMvc
			.perform(get("/api/sessions").session(session).param("from", "2026-10-01").param("to", "2026-10-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].title").value("10월 일정"));
	}

	@Test
	@DisplayName("일정을 수정하며 상태를 완료로 바꿀 수 있다")
	void updateAndChangeStatus() throws Exception {
		MockHttpSession session = signup("planner");
		long id = createPlan(session, "원래 제목", "2026-10-20");

		this.mockMvc
			.perform(patch("/api/sessions/" + id).session(session).contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"바뀐 제목\",\"plannedDate\":\"2026-10-21\",\"ruleName\":\"던전월드\","
						+ "\"gm\":true,\"status\":\"DONE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("바뀐 제목"))
			.andExpect(jsonPath("$.plannedDate").value("2026-10-21"))
			.andExpect(jsonPath("$.gm").value(true))
			.andExpect(jsonPath("$.status").value("DONE"));
	}

	@Test
	@DisplayName("gm·메모를 생략해도 201 이고 gm 은 false 다 (선택 항목)")
	void createWithoutOptionalFields() throws Exception {
		MockHttpSession session = signup("planner");

		this.mockMvc
			.perform(post("/api/sessions").session(session).contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"짧은 일정\",\"plannedDate\":\"2026-09-15\",\"ruleName\":\"던전월드\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.gm").value(false))
			.andExpect(jsonPath("$.memo").doesNotExist());
	}

	@Test
	@DisplayName("남의 일정은 수정·삭제할 수 없고, 소유자는 삭제할 수 있다")
	void cannotTouchOthersPlan() throws Exception {
		MockHttpSession owner = signup("owner");
		MockHttpSession other = signup("other");
		long id = createPlan(owner, "내 일정", "2026-10-20");

		this.mockMvc
			.perform(patch("/api/sessions/" + id).session(other).contentType(MediaType.APPLICATION_JSON)
				.content(planJson("가로채기", "2026-10-20", "던전월드", null, "false", null)))
			.andExpect(status().isForbidden());

		this.mockMvc.perform(delete("/api/sessions/" + id).session(other)).andExpect(status().isForbidden());

		this.mockMvc.perform(delete("/api/sessions/" + id).session(owner)).andExpect(status().isNoContent());

		this.mockMvc
			.perform(get("/api/sessions").session(owner).param("from", "2026-10-01").param("to", "2026-10-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("로그인하지 않으면 401 을 반환한다")
	void requiresLogin() throws Exception {
		this.mockMvc.perform(get("/api/sessions").param("from", "2026-10-01").param("to", "2026-10-31"))
			.andExpect(status().isUnauthorized());

		this.mockMvc
			.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
				.content(planJson("제목", "2026-10-20", "던전월드", null, "false", null)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("제목이 비면 400, 조회 기간이 너무 길면 400 을 반환한다")
	void badRequests() throws Exception {
		MockHttpSession session = signup("planner");

		this.mockMvc
			.perform(post("/api/sessions").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(planJson("   ", "2026-10-20", "던전월드", null, "false", null)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("title"));

		this.mockMvc
			.perform(get("/api/sessions").session(session).param("from", "2026-01-01").param("to", "2026-12-31"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("to"));
	}

	@Test
	@DisplayName("함께한 사람을 추가하면 목록 응답에 함께 나온다")
	void addParticipant() throws Exception {
		MockHttpSession owner = signup("planner");
		signup("partner");
		long id = createPlan(owner, "정기 캠페인", "2026-10-20");

		addParticipant(owner, id, "partner");

		this.mockMvc
			.perform(get("/api/sessions").session(owner).param("from", "2026-10-01").param("to", "2026-10-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].participants.length()").value(1))
			.andExpect(jsonPath("$[0].participants[0].handle").value("partner"))
			.andExpect(jsonPath("$[0].participants[0].nickname").value("고드세"));
	}

	@Test
	@DisplayName("참가자 추가는 빈 핸들 400, 없는 핸들 404, 나 자신 400, 중복 409, 남의 일정 403")
	void participantErrors() throws Exception {
		MockHttpSession owner = signup("planner");
		signup("partner");
		MockHttpSession other = signup("other");
		long id = createPlan(owner, "정기 캠페인", "2026-10-20");

		// 빈 핸들 — 요청 검증(@NotBlank)
		this.mockMvc
			.perform(post("/api/sessions/" + id + "/participants").session(owner)
				.contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("handle"));

		// 없는 사용자
		this.mockMvc
			.perform(post("/api/sessions/" + id + "/participants").session(owner)
				.contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"ghost\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.errors[0].field").value("handle"));

		// 나 자신
		this.mockMvc
			.perform(post("/api/sessions/" + id + "/participants").session(owner)
				.contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"planner\"}"))
			.andExpect(status().isBadRequest());

		// 중복
		addParticipant(owner, id, "partner");
		this.mockMvc
			.perform(post("/api/sessions/" + id + "/participants").session(owner)
				.contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"partner\"}"))
			.andExpect(status().isConflict());

		// 남의 일정
		this.mockMvc
			.perform(post("/api/sessions/" + id + "/participants").session(other)
				.contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"partner\"}"))
			.andExpect(status().isForbidden());

		// 없는 일정
		this.mockMvc
			.perform(post("/api/sessions/999999/participants").session(owner)
				.contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"partner\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("참가자를 빼면 목록에서 사라지고, 없는 참가자를 빼면 404")
	void removeParticipant() throws Exception {
		MockHttpSession owner = signup("planner");
		signup("partner");
		long id = createPlan(owner, "정기 캠페인", "2026-10-20");
		addParticipant(owner, id, "partner");

		this.mockMvc.perform(delete("/api/sessions/" + id + "/participants/partner").session(owner))
			.andExpect(status().isNoContent());

		this.mockMvc
			.perform(get("/api/sessions").session(owner).param("from", "2026-10-01").param("to", "2026-10-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].participants.length()").value(0));

		this.mockMvc.perform(delete("/api/sessions/" + id + "/participants/partner").session(owner))
			.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("참가자가 있는 일정도 삭제할 수 있다")
	void deletePlanWithParticipants() throws Exception {
		MockHttpSession owner = signup("planner");
		signup("partner");
		long id = createPlan(owner, "정기 캠페인", "2026-10-20");
		addParticipant(owner, id, "partner");

		this.mockMvc.perform(delete("/api/sessions/" + id).session(owner)).andExpect(status().isNoContent());

		this.mockMvc
			.perform(get("/api/sessions").session(owner).param("from", "2026-10-01").param("to", "2026-10-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("참가자 API 도 로그인이 필요하다")
	void participantsRequireLogin() throws Exception {
		this.mockMvc
			.perform(post("/api/sessions/1/participants").contentType(MediaType.APPLICATION_JSON)
				.content("{\"handle\":\"anyone\"}"))
			.andExpect(status().isUnauthorized());

		this.mockMvc.perform(delete("/api/sessions/1/participants/anyone")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("참가자 추가는 핸들 대소문자를 구분하지 않는다")
	void participantIgnoresCase() throws Exception {
		MockHttpSession owner = signup("planner");
		signup("partner");
		long id = createPlan(owner, "정기 캠페인", "2026-10-20");

		addParticipant(owner, id, "Partner");

		this.mockMvc
			.perform(get("/api/sessions").session(owner).param("from", "2026-10-01").param("to", "2026-10-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].participants[0].handle").value("partner"));
	}

	private void addParticipant(MockHttpSession owner, long id, String handle) throws Exception {
		this.mockMvc
			.perform(post("/api/sessions/" + id + "/participants").session(owner)
				.contentType(MediaType.APPLICATION_JSON).content("{\"handle\":\"" + handle + "\"}"))
			.andExpect(status().isNoContent());
	}

	private long createPlan(MockHttpSession session, String title, String date) throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/sessions").session(session).contentType(MediaType.APPLICATION_JSON)
				.content(planJson(title, date, "던전월드", null, "false", null)))
			.andExpect(status().isCreated())
			.andReturn();

		return idOf(result);
	}

	private static String planJson(String title, String date, String rule, String scenario, String gm, String memo) {
		StringBuilder json = new StringBuilder("{\"title\":\"").append(title).append("\",");
		json.append("\"plannedDate\":\"").append(date).append("\",");
		json.append("\"ruleName\":\"").append(rule).append("\",");
		json.append("\"gm\":").append(gm);
		if (scenario != null) {
			json.append(",\"scenarioName\":\"").append(scenario).append("\"");
		}
		if (memo != null) {
			json.append(",\"memo\":\"").append(memo).append("\"");
		}
		json.append("}");
		return json.toString();
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
