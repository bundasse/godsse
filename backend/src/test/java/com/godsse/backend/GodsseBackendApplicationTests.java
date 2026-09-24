package com.godsse.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 컨텍스트가 정상 기동하는지 확인한다.
 *
 * <p>
 * {@code @ActiveProfiles("test")} 로 application-test.properties(메모리 DB)를 함께 적용한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class GodsseBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
