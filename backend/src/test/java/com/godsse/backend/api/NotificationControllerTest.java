package com.godsse.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 API 테스트.
 *
 * <p>
 * 알림은 다른 기능의 곁다리로 만들어진다. 그래서 이 테스트는 <b>"무엇이 알림을 만드는가"</b>와
 * <b>"무엇이 알림을 만들지 않는가"</b>를 함께 확인한다.
 * <ul>
 * <li>감상 작성 → 받은 사람 / 좋아요 → 감상 작성자 / 답장 → 작성자·받은 사람 / 팔로우 → 대상</li>
 * <li>내 활동(내 감상에 내가 좋아요)은 알림을 만들지 않는다</li>
 * <li>좋아요 취소·언팔로우는 알림도 지운다(사실과 다른 알림을 남기지 않는다)</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

	private static final String LONG_BODY = "가".repeat(120);

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("감상을 받으면 받은 사람에게 알림이 생기고, 쓴 사람에게는 생기지 않는다")
	void reviewNotifiesRecipient() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");

		long reviewId = createReview(writer, "partner");

		this.mockMvc.perform(get("/api/notifications").session(partner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.unreadCount").value(1))
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].type").value("REVIEW"))
			.andExpect(jsonPath("$.items[0].actor.handle").value("writer"))
			.andExpect(jsonPath("$.items[0].reviewId").value(reviewId))
			.andExpect(jsonPath("$.items[0].read").value(false));

		// 감상을 쓴 사람은 자기 활동으로 알림을 받지 않는다
		this.mockMvc.perform(get("/api/notifications").session(writer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("좋아요는 감상 작성자에게 알림을 만들고, 취소하면 알림도 사라진다")
	void likeNotifiesAuthorAndUnlikeRemovesIt() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");
		MockHttpSession liker = signup("liker");
		long reviewId = createReview(writer, "partner");

		this.mockMvc.perform(post("/api/reviews/" + reviewId + "/like").session(liker))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/notifications").session(writer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.unreadCount").value(1))
			.andExpect(jsonPath("$.items[0].type").value("LIKE"))
			.andExpect(jsonPath("$.items[0].actor.handle").value("liker"));

		this.mockMvc.perform(delete("/api/reviews/" + reviewId + "/like").session(liker))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/notifications").session(writer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.unreadCount").value(0))
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("답장은 감상 작성자와 받은 사람 모두에게 알린다")
	void replyNotifiesAuthorAndRecipient() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		MockHttpSession reader = signup("reader");
		long reviewId = createReview(writer, "partner");

		addReply(reader, reviewId, "저도 읽고 싶어요!");

		// 감상 작성자
		this.mockMvc.perform(get("/api/notifications").session(writer))
			.andExpect(jsonPath("$.items[0].type").value("REPLY"))
			.andExpect(jsonPath("$.items[0].actor.handle").value("reader"));

		// 감상을 받은 사람
		this.mockMvc.perform(get("/api/notifications").session(partner))
			.andExpect(jsonPath("$.items[0].type").value("REPLY"))
			.andExpect(jsonPath("$.items[0].actor.handle").value("reader"));

		// 답장을 쓴 사람은 알림을 받지 않는다
		this.mockMvc.perform(get("/api/notifications").session(reader))
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("팔로우는 대상에게 알림을 만들고, 언팔로우하면 알림도 사라진다")
	void followNotifiesTargetAndUnfollowRemovesIt() throws Exception {
		MockHttpSession follower = signup("follower");
		MockHttpSession target = signup("target");

		this.mockMvc.perform(post("/api/users/target/follow").session(follower)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/notifications").session(target))
			.andExpect(jsonPath("$.unreadCount").value(1))
			.andExpect(jsonPath("$.items[0].type").value("FOLLOW"))
			.andExpect(jsonPath("$.items[0].actor.handle").value("follower"))
			.andExpect(jsonPath("$.items[0].reviewId").doesNotExist());

		this.mockMvc.perform(delete("/api/users/target/follow").session(follower)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/notifications").session(target))
			.andExpect(jsonPath("$.unreadCount").value(0))
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("내 감상에 내가 좋아요해도 알림은 생기지 않는다")
	void selfLikeDoesNotNotify() throws Exception {
		MockHttpSession writer = signup("writer");
		signup("partner");
		long reviewId = createReview(writer, "partner");

		this.mockMvc.perform(post("/api/reviews/" + reviewId + "/like").session(writer))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/notifications").session(writer))
			.andExpect(jsonPath("$.unreadCount").value(0))
			.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	@DisplayName("하나 읽음과 모두 읽음이 안 읽은 개수에 반영된다")
	void markReadAndMarkAllRead() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		MockHttpSession liker = signup("liker");

		// writer 는 감상을 받고(REVIEW), 자기 감상에 좋아요를 받는다(LIKE)
		createReview(partner, "writer");
		long reviewId = createReview(writer, "partner");

		this.mockMvc.perform(post("/api/reviews/" + reviewId + "/like").session(liker))
			.andExpect(status().isNoContent());

		// 알림 2건(감상 + 좋아요) 중 최신 1건만 읽음 처리
		long notificationId = firstNotificationId(writer);

		this.mockMvc.perform(post("/api/notifications/" + notificationId + "/read").session(writer))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/notifications").session(writer))
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.unreadCount").value(1));

		this.mockMvc.perform(post("/api/notifications/read-all").session(writer)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/notifications").session(writer))
			.andExpect(jsonPath("$.unreadCount").value(0))
			.andExpect(jsonPath("$.items[0].read").value(true))
			.andExpect(jsonPath("$.items[1].read").value(true));
	}

	@Test
	@DisplayName("남의 알림은 읽음 처리할 수 없고 404 다")
	void cannotReadOthersNotification() throws Exception {
		MockHttpSession writer = signup("writer");
		MockHttpSession partner = signup("partner");
		createReview(writer, "partner");

		long notificationId = firstNotificationId(partner);

		this.mockMvc.perform(post("/api/notifications/" + notificationId + "/read").session(writer))
			.andExpect(status().isNotFound());

		// 내 알림은 그대로 남아 있다
		this.mockMvc.perform(get("/api/notifications").session(partner))
			.andExpect(jsonPath("$.unreadCount").value(1));
	}

	@Test
	@DisplayName("알림 API 는 로그인이 필요하다")
	void requiresLogin() throws Exception {
		this.mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(get("/api/notifications/unread-count")).andExpect(status().isUnauthorized());
		this.mockMvc.perform(post("/api/notifications/read-all")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("실시간 스트림은 로그인이 필요하고, 연결되면 현재 개수를 보낸다")
	void streamSendsCountOnConnect() throws Exception {
		MockHttpSession writer = signup("writer");

		this.mockMvc.perform(get("/api/notifications/stream")).andExpect(status().isUnauthorized());

		MvcResult result = this.mockMvc.perform(get("/api/notifications/stream").session(writer))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted())
			.andReturn();

		assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
		assertThat(result.getResponse().getContentAsString()).contains("event:connected", "\"count\":0");

		closeStream(result);
	}

	@Test
	@DisplayName("알림이 생기면 열려 있는 스트림으로 바뀐 개수를 밀어 준다")
	// AFTER_COMMIT 리스너는 "진짜 커밋"이 있어야 돌기 때문에, 이 테스트만 테스트 트랜잭션을 쓰지 않는다.
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void streamPushesChangedCount() throws Exception {
		MockHttpSession writer = signup("ssewriter");
		MockHttpSession partner = signup("ssepartner");

		MvcResult stream = this.mockMvc.perform(get("/api/notifications/stream").session(partner))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted())
			.andReturn();

		// 감상이 만들어지면 받은 사람(partner)의 스트림으로 changed 가 나간다
		createReview(writer, "ssepartner");

		assertThat(stream.getResponse().getContentAsString()).contains("event:changed", "\"count\":1");

		closeStream(stream);
	}

	@Test
	@DisplayName("커밋 전에는 스트림으로 보내지 않는다")
	void noPushBeforeCommit() throws Exception {
		// 이 테스트는 클래스의 @Transactional 을 그대로 쓴다 → 테스트가 끝날 때 롤백된다.
		// 그래서 알림은 만들어지지만 커밋이 없고, AFTER_COMMIT 리스너도 돌지 않는다.
		MockHttpSession writer = signup("txwriter");
		MockHttpSession partner = signup("txpartner");

		MvcResult stream = this.mockMvc.perform(get("/api/notifications/stream").session(partner))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted())
			.andReturn();

		createReview(writer, "txpartner");

		// 연결 직후의 connected 만 있고, changed 는 아직 나가지 않는다
		assertThat(stream.getResponse().getContentAsString()).contains("event:connected")
			.doesNotContain("event:changed");

		closeStream(stream);
	}

	/** 열린 스트림을 닫는다(연결이 다른 테스트로 새지 않게). */
	private static void closeStream(MvcResult result) {
		if (result.getRequest().isAsyncStarted()) {
			((MockAsyncContext) result.getRequest().getAsyncContext()).complete();
		}
	}

	/** writer 가 partner 에게 감상을 남기고 id 를 돌려준다. */
	private long createReview(MockHttpSession writer, String toHandle) throws Exception {
		String json = "{\"toHandle\":\"" + toHandle + "\",\"playedOn\":\"2026-10-01\",\"ruleName\":\"던전월드\",\"body\":\""
				+ LONG_BODY + "\"}";

		MvcResult result = this.mockMvc
			.perform(post("/api/reviews").session(writer).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn();

		return idOf(result);
	}

	private void addReply(MockHttpSession session, long reviewId, String body) throws Exception {
		this.mockMvc
			.perform(post("/api/reviews/" + reviewId + "/replies").session(session)
				.contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + body + "\"}"))
			.andExpect(status().isCreated());
	}

	/** 가장 최근 알림의 id. */
	private long firstNotificationId(MockHttpSession session) throws Exception {
		MvcResult result = this.mockMvc.perform(get("/api/notifications").session(session))
			.andExpect(status().isOk())
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
