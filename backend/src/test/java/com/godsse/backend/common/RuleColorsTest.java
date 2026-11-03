package com.godsse.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 룰 색 기본 팔레트 테스트.
 *
 * <p>
 * 히트맵 칸 색은 사용자가 고르지 않으면 이름에서 계산한다. 계산 결과가 흔들리면
 * 화면을 새로 고칠 때마다 색이 바뀌므로, <b>같은 이름 → 같은 색</b> 을 꼭 확인한다.
 */
class RuleColorsTest {

	@Test
	@DisplayName("기본 팔레트는 모두 #RRGGBB 형식이다")
	void paletteIsValidHex() {
		assertThat(RuleColors.palette()).hasSize(10)
			.allSatisfy(color -> assertThat(RuleColors.isValid(color)).isTrue());
	}

	@Test
	@DisplayName("같은 룰 이름은 언제나 같은 색이다")
	void defaultColorIsStable() {
		assertThat(RuleColors.defaultColor("던전월드")).isEqualTo(RuleColors.defaultColor("던전월드"));
		assertThat(RuleColors.defaultColor("크툴루의 부름 7판"))
			.isEqualTo(RuleColors.defaultColor("크툴루의 부름 7판"));
	}

	@Test
	@DisplayName("기본 색은 항상 팔레트 안의 값이다")
	void defaultColorComesFromPalette() {
		List<String> palette = RuleColors.palette();

		for (String ruleName : List.of("크툴루의 부름 7판", "던전월드", "인세인", "마기카로기아", "", "a")) {
			assertThat(palette).contains(RuleColors.defaultColor(ruleName));
		}
	}

	@Test
	@DisplayName("이름이 없어도 예외 없이 팔레트 색을 돌려준다")
	void defaultColorHandlesNull() {
		assertThat(RuleColors.palette()).contains(RuleColors.defaultColor(null));
	}

	@Test
	@DisplayName("색 형식 검사는 #RRGGBB 만 통과시킨다")
	void validatesHexColor() {
		assertThat(RuleColors.isValid("#16a34a")).isTrue();
		assertThat(RuleColors.isValid("#16A34A")).isTrue();

		assertThat(RuleColors.isValid("16a34a")).isFalse();
		assertThat(RuleColors.isValid("#16a34")).isFalse();
		assertThat(RuleColors.isValid("#gggggg")).isFalse();
		assertThat(RuleColors.isValid(null)).isFalse();
	}

}
