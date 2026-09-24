package com.godsse.backend.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 업로드한 프로필 이미지를 {@code /uploads/**} URL 로 제공한다.
 *
 * <p>
 * 예: {@code backend/uploads/profiles/a1b2.png} 파일이
 * 브라우저에서 {@code /uploads/profiles/a1b2.png} 로 접근할 수 있다.
 * (Java 주석에서도 백슬래시+u 조합은 유니코드 이스케이프로 해석되므로 경로 예시는 슬래시로 적는다)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final Path uploadDirectory;

	public WebConfig(Path uploadDirectory) {
		this.uploadDirectory = uploadDirectory;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// 리소스 위치는 반드시 "/" 로 끝나야 한다. (파일 경로 → file:// URL 변환)
		String location = this.uploadDirectory.toUri().toString();
		if (!location.endsWith("/")) {
			location = location + "/";
		}

		registry.addResourceHandler("/uploads/**").addResourceLocations(location);
	}

}
