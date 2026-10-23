package com.godsse.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 핸들 정규화 규칙.
 *
 * <p>
 * 저장(회원가입)과 조회가 <b>같은 규칙</b>을 쓰도록 한 곳에 모아 두었다.
 * 이 함수가 바뀌면 가입·로그인·프로필·감상·팔로우·참가자 조회가 모두 함께 바뀐다.
 */
class HandlesTest {

	@Test
	@DisplayName("앞뒤 공백을 지우고 소문자로 바꾼다")
	void normalizes() {
		assertThat(Handles.normalize("  Godsse  ")).isEqualTo("godsse");
		assertThat(Handles.normalize("GODSSE")).isEqualTo("godsse");
		assertThat(Handles.normalize("godsse")).isEqualTo("godsse");
	}

	@Test
	@DisplayName("null 은 빈 문자열로 바꾼다")
	void nullBecomesEmpty() {
		assertThat(Handles.normalize(null)).isEmpty();
	}

}
