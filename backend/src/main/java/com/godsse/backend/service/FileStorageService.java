package com.godsse.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.godsse.backend.common.exception.BadRequestException;

/**
 * 업로드한 파일을 서버 폴더에 저장하고, 브라우저가 접근할 경로를 돌려준다.
 *
 * <p>
 * 저장 위치는 {@code app.upload.dir}(기본 {@code ./uploads}) 이고, 파일은
 * {@code profiles/} 아래에 <b>UUID 이름</b>으로 저장한다.
 *
 * <p>
 * 왜 원래 파일 이름을 쓰지 않을까?
 * <ul>
 * <li>같은 이름의 파일이 서로를 덮어쓰는 문제를 막는다.</li>
 * <li>{@code ../../secret.txt} 같은 이름으로 폴더를 벗어나는 공격(경로 조작)을 막는다.</li>
 * <li>파일 이름이 매번 바뀌므로 브라우저 캐시가 옛 이미지를 보여 주는 문제도 사라진다.</li>
 * </ul>
 */
@Service
public class FileStorageService {

	private static final String PROFILE_SUBDIRECTORY = "profiles";

	/** 업로드 후 브라우저가 접근할 때 쓰는 URL 접두사. WebConfig 의 매핑과 같아야 한다. */
	private static final String PUBLIC_PREFIX = "/uploads/";

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/gif",
			"image/webp");

	private final Path uploadDirectory;
	private final long maxBytes;

	public FileStorageService(Path uploadDirectory, @Value("${app.upload.max-bytes}") long maxBytes) {
		this.uploadDirectory = uploadDirectory;
		this.maxBytes = maxBytes;
	}

	/**
	 * 프로필 이미지를 저장하고 공개 경로를 돌려준다.
	 *
	 * @param file 업로드된 파일
	 * @return 예: {@code /uploads/profiles/9f2c...c1.png}
	 * @throws BadRequestException 파일이 없거나, 이미지가 아니거나, 크기를 넘을 때
	 */
	public String storeProfileImage(MultipartFile file) {
		validate(file);

		String fileName = UUID.randomUUID().toString().replace("-", "") + "."
				+ extractExtension(file.getOriginalFilename());
		Path target = this.uploadDirectory.resolve(PROFILE_SUBDIRECTORY).resolve(fileName);

		try {
			Files.createDirectories(target.getParent());
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			throw new IllegalStateException("업로드 파일을 저장하지 못했습니다: " + fileName, exception);
		}

		return PUBLIC_PREFIX + PROFILE_SUBDIRECTORY + "/" + fileName;
	}

	/**
	 * 이전 프로필 이미지를 지운다.
	 *
	 * <p>
	 * 지우지 못해도(이미 없거나 사용 중) 사용자의 요청 자체는 성공했으므로 예외를 던지지 않는다.
	 * 용량이 쌓이는 것을 막기 위한 정리 작업이기 때문이다.
	 */
	public void deleteProfileImage(String publicUrl) {
		if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) {
			return;
		}

		Path target = this.uploadDirectory.resolve(publicUrl.substring(PUBLIC_PREFIX.length())).normalize();
		if (!target.startsWith(this.uploadDirectory)) {
			// uploads 밖을 가리키는 값이면 지우지 않는다.
			return;
		}

		try {
			Files.deleteIfExists(target);
		} catch (IOException ignored) {
			// 정리 실패는 무시한다.
		}
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("file", "이미지 파일을 선택해 주세요.");
		}
		if (file.getSize() > this.maxBytes) {
			throw new BadRequestException("file",
					"이미지는 " + (this.maxBytes / 1024 / 1024) + "MB 이하만 올릴 수 있습니다.");
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
			throw new BadRequestException("file", "PNG·JPG·GIF·WEBP 이미지만 올릴 수 있습니다.");
		}
		if (!ALLOWED_EXTENSIONS.contains(extractExtension(file.getOriginalFilename()))) {
			throw new BadRequestException("file", "PNG·JPG·GIF·WEBP 이미지만 올릴 수 있습니다.");
		}
	}

	/** {@code photo.PNG} → {@code png}. 점이 없거나 끝이 점이면 빈 문자열. */
	private static String extractExtension(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		int dot = originalFilename.lastIndexOf('.');
		if (dot < 0 || dot == originalFilename.length() - 1) {
			return "";
		}
		return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

}
