package com.godsse.backend.api;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HelloControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("GET /api/hello 는 200 과 message, timestamp 를 반환한다")
	void hello() throws Exception {
		this.mockMvc.perform(get("/api/hello").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").isNotEmpty())
			.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	@DisplayName("GET /api/hello/{name} 은 이름이 포함된 message 를 반환한다")
	void helloTo() throws Exception {
		this.mockMvc.perform(get("/api/hello/godsse").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("godsse님, 반갑습니다!"));
	}

	@Test
	@DisplayName("POST /api/hello/echo 는 공백을 제거한 message 와 길이를 반환한다")
	void echo() throws Exception {
		this.mockMvc
			.perform(post("/api/hello/echo").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"godsse\",\"message\":\"  hello  \"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("godsse"))
			.andExpect(jsonPath("$.message").value("hello"))
			.andExpect(jsonPath("$.length").value(5));
	}

	@Test
	@DisplayName("POST /api/hello/echo 는 검증 실패 시 400 과 필드 오류를 반환한다")
	void echoWithInvalidRequest() throws Exception {
		this.mockMvc
			.perform(post("/api/hello/echo").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\",\"message\":\"정상 메시지\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").isNotEmpty())
			.andExpect(jsonPath("$.errors.length()").value(1))
			.andExpect(jsonPath("$.errors[0].field").value("name"));
	}

}
