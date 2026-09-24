package com.godsse.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 업로드 파일 저장 위치를 빈(bean)으로 등록한다.
 *
 * <p>
 * 설정값은 {@code application.properties} 의 {@code app.upload.dir} 하나만 두고,
 * 실제로 쓰는 곳(정적 파일 제공, 파일 저장 서비스)은 이 빈을 주입받아 같은 경로를 공유한다.
 *
 * <p>
 * 시작할 때 폴더가 없으면 만들어 둔다. 권한 문제 등으로 만들 수 없으면 여기서 바로 실패하므로
 * "업로드할 때가 되어서야 오류가 나는" 상황을 피할 수 있다.
 */
@Configuration
public class UploadConfig {

	@Bean
	public Path uploadDirectory(@Value("${app.upload.dir}") String uploadDirectory) {
		Path directory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
		try {
			Files.createDirectories(directory);
		} catch (IOException exception) {
			throw new IllegalStateException("업로드 폴더를 만들 수 없습니다: " + directory, exception);
		}
		return directory;
	}

}
