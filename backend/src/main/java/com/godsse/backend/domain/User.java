package com.godsse.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 서비스 사용자 한 명.
 *
 * <p>
 * {@code handle} 은 아이디에 해당하는 고유 이름으로, URL({@code /u/godsse})과 검색에 쓰인다.
 * 화면에 보이는 이름은 {@code nickname} 이며 중복될 수 있다.
 *
 * <p>
 * 비밀번호는 {@code passwordHash} 에 BCrypt 로 변환한 값만 저장한다. 원문은 어디에도 남기지 않는다.
 */
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 고유 핸들. 소문자·숫자·밑줄만 사용한다(형식 검증은 요청 DTO 에서 한다). */
	@Column(nullable = false, unique = true, length = 20)
	private String handle;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	/** BCrypt 해시 값(60자). */
	@Column(nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 20)
	private String nickname;

	/** 자기소개 메시지(선택). */
	@Column(length = 300)
	private String bio;

	/** 프로필 이미지 공개 경로(예: {@code /uploads/profiles/xxxx.png}). 없으면 null. */
	@Column(length = 300)
	private String profileImageUrl;

	/** JPA 가 사용하는 기본 생성자. 직접 호출하지 않는다. */
	protected User() {
	}

	public User(String handle, String email, String passwordHash, String nickname) {
		this.handle = handle;
		this.email = email;
		this.passwordHash = passwordHash;
		this.nickname = nickname;
	}

	/** 프로필 편집 — 닉네임·자기소개·프로필 이미지를 한 번에 바꾼다. */
	public void updateProfile(String nickname, String bio, String profileImageUrl) {
		this.nickname = nickname;
		this.bio = bio;
		this.profileImageUrl = profileImageUrl;
	}

	public Long getId() {
		return this.id;
	}

	public String getHandle() {
		return this.handle;
	}

	public String getEmail() {
		return this.email;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public String getNickname() {
		return this.nickname;
	}

	public String getBio() {
		return this.bio;
	}

	public String getProfileImageUrl() {
		return this.profileImageUrl;
	}

}
