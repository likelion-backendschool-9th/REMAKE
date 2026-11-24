package com.remake.user.adapter.out.persistence;

import java.time.LocalDateTime;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.user.domain.AccountDeletionRequest;

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
@Table(name = "account_deletion_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AccountDeletionRequestJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "code_hash", nullable = false, length = 64)
	private String codeHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(nullable = false)
	private int attempts;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	static AccountDeletionRequestJpaEntity from(AccountDeletionRequest request) {
		AccountDeletionRequestJpaEntity entity = new AccountDeletionRequestJpaEntity();
		entity.id = request.id();
		entity.apply(request);
		return entity;
	}

	void apply(AccountDeletionRequest request) {
		this.userId = request.userId();
		this.codeHash = request.codeHash();
		this.expiresAt = request.expiresAt();
		this.attempts = request.attempts();
		this.verifiedAt = request.verifiedAt();
	}

	AccountDeletionRequest toDomain() {
		return AccountDeletionRequest.restore(id, userId, codeHash, expiresAt, attempts, verifiedAt, getCreatedAt());
	}
}
