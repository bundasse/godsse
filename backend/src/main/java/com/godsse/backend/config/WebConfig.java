package com.godsse.backend.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.godsse.backend.common.LoginUserArgumentResolver;

/**
 * 업로드한 프로필 이미지를 {@code /uploads/**} URL 로 제공하고,
 * 로그인 사용자 주입기({@link LoginUserArgumentResolver})를 등록한다.
 *
 * <p>
 * 예: {@code backend/uploads/profiles/a1b2.png} 파일이
 * 브라우저에서 {@code /uploads/profiles/a1b2.png} 로 접근할 수 있다.
 * (Java 주석에서도 백슬래시+u 조합은 유니코드 이스케이프로 해석되므로 경로 예시는 슬래시로 적는다)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final Path uploadDirectory;
	private final LoginUserArgumentResolver loginUserArgumentResolver;

	public WebConfig(Path uploadDirectory, LoginUserArgumentResolver loginUserArgumentResolver) {
		this.uploadDirectory = uploadDirectory;
		this.loginUserArgumentResolver = loginUserArgumentResolver;
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

	/**
	 * {@code @LoginUser} 가 붙은 파라미터를 처리할 변환기를 등록한다.
	 *
	 * <p>
	 * 등록하지 않으면 Spring 은 {@code @LoginUser User user} 를 일반 파라미터로 보려 하기 때문에
	 * 반드시 여기서 추가해 주어야 한다.
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(this.loginUserArgumentResolver);
	}

}
