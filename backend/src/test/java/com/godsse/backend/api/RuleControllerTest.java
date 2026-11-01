package com.godsse.backend.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.godsse.backend.config.RuleSystemSeeder;
import com.godsse.backend.repository.RuleSystemRepository;

/**
 * 룰 자동완성 API 테스트.
 *
 * <p>
 * 룰 목록은 로그인 없이 볼 수 있는 참조 데이터이므로 인증 없이 호출한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RuleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RuleSystemRepository ruleSystemRepository;

	@Autowired
	private RuleSystemSeeder ruleSystemSeeder;

	@Test
	@DisplayName("기본 룰 목록을 약어·이름으로 돌려주고 정렬 순서를 지킨다")
	void listDefaults() throws Exception {
		this.mockMvc.perform(get("/api/rules"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(8))
			.andExpect(jsonPath("$[0].code").value("COC7"))
			.andExpect(jsonPath("$[0].label").value("크툴루의 부름 7판"))
			.andExpect(jsonPath("$[3].code").value("DW"));
	}

	@Test
	@DisplayName("시드를 다시 실행해도 룰이 중복되지 않는다")
	void seedingIsIdempotent() throws Exception {
		long before = this.ruleSystemRepository.count();

		this.ruleSystemSeeder.run(new DefaultApplicationArguments(new String[0]));

		assertThat(this.ruleSystemRepository.count()).isEqualTo(before);
		assertThat(this.ruleSystemRepository.findByCode("COC7")).isPresent();
	}

}
