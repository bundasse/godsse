package com.godsse.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트엔드(개발 서버)에서 백엔드 API 를 직접 호출할 때 필요한 CORS 설정.
 *
 * <p>
 * 허용 오리진은 {@code app.cors.allowed-origins} 프로퍼티로 관리한다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
		this.allowedOrigins = allowedOrigins.split(",");
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins(this.allowedOrigins)
			.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(true)
			.maxAge(3600);
	}

}
