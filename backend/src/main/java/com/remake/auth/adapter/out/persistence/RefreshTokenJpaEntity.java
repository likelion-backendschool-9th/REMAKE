package com.remake.auth.adapter.out.persistence;

import java.time.LocalDateTime;

import com.remake.auth.domain.RefreshToken;
import com.remake.global.common.BaseCreatedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RefreshTokenJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	static RefreshTokenJpaEntity from(RefreshToken token) {
		RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
		entity.id = token.id();
		entity.apply(token);
		return entity;
	}

	void apply(RefreshToken token) {
		this.userId = token.userId();
		this.tokenHash = token.tokenHash();
		this.expiresAt = token.expiresAt();
		this.revokedAt = token.revokedAt();
	}

	RefreshToken toDomain() {
		return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt);
	}
}
