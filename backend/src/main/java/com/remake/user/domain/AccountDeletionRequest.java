package com.remake.user.domain;

import java.time.LocalDateTime;

/**
 * 계정 삭제 이메일 인증번호 한 건. 평문 번호는 갖지 않는다.
 * attempts가 {@link #MAX_WRONG_ATTEMPTS} 이상이면 잠긴다(에러 정의서: 5회 넘게 틀림).
 */
public class AccountDeletionRequest {

	/** 이 횟수 이상 틀리면 DELETION_CODE_LOCKED */
	public static final int MAX_WRONG_ATTEMPTS = 5;

	/** 발송 후 유효 시간(분) */
	public static final int VALID_MINUTES = 5;

	private final Long id;
	private final Long userId;
	private final String codeHash;
	private final LocalDateTime expiresAt;
	private int attempts;
	private LocalDateTime verifiedAt;
	private final LocalDateTime createdAt;

	private AccountDeletionRequest(Long id, Long userId, String codeHash, LocalDateTime expiresAt, int attempts,
			LocalDateTime verifiedAt, LocalDateTime createdAt) {
		this.id = id;
		this.userId = userId;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
		this.attempts = attempts;
		this.verifiedAt = verifiedAt;
		this.createdAt = createdAt;
	}

	public static AccountDeletionRequest issue(Long userId, String codeHash, LocalDateTime expiresAt,
			LocalDateTime createdAt) {
		if (userId == null || codeHash == null || codeHash.isBlank() || expiresAt == null || createdAt == null) {
			throw new IllegalArgumentException("인증번호 요청 값이 올바르지 않습니다.");
		}
		return new AccountDeletionRequest(null, userId, codeHash, expiresAt, 0, null, createdAt);
	}

	public static AccountDeletionRequest restore(Long id, Long userId, String codeHash, LocalDateTime expiresAt,
			int attempts, LocalDateTime verifiedAt, LocalDateTime createdAt) {
		return new AccountDeletionRequest(id, userId, codeHash, expiresAt, attempts, verifiedAt, createdAt);
	}

	public boolean isLocked() {
		return attempts >= MAX_WRONG_ATTEMPTS;
	}

	public boolean isExpired(LocalDateTime now) {
		return !expiresAt.isAfter(now);
	}

	public boolean isVerified() {
		return verifiedAt != null;
	}

	/** 틀린 입력을 반영한다. 잠금 한도에 도달하면 true. */
	public boolean registerWrongAttempt() {
		if (isLocked() || isVerified()) {
			return isLocked();
		}
		attempts += 1;
		return isLocked();
	}

	public int remainingAttempts() {
		return Math.max(0, MAX_WRONG_ATTEMPTS - attempts);
	}

	public void markVerified(LocalDateTime verifiedAt) {
		if (verifiedAt == null) {
			throw new IllegalArgumentException("확인 시각이 필요합니다.");
		}
		this.verifiedAt = verifiedAt;
	}

	public Long id() {
		return id;
	}

	public Long userId() {
		return userId;
	}

	public String codeHash() {
		return codeHash;
	}

	public LocalDateTime expiresAt() {
		return expiresAt;
	}

	public int attempts() {
		return attempts;
	}

	public LocalDateTime verifiedAt() {
		return verifiedAt;
	}

	public LocalDateTime createdAt() {
		return createdAt;
	}
}
