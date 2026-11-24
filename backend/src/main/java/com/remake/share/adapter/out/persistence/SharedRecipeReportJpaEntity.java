package com.remake.share.adapter.out.persistence;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.share.domain.SharedRecipeReportReason;
import com.remake.share.domain.SharedRecipeReportStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shared_recipe_reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SharedRecipeReportJpaEntity extends BaseCreatedEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private Long sharedRecipeId;
	@Column(nullable = false)
	private Long reporterId;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SharedRecipeReportReason reason;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SharedRecipeReportStatus status;

	static SharedRecipeReportJpaEntity create(Long userId, Long sharedRecipeId, SharedRecipeReportReason reason) {
		SharedRecipeReportJpaEntity e = new SharedRecipeReportJpaEntity();
		e.reporterId = userId;
		e.sharedRecipeId = sharedRecipeId;
		e.reason = reason;
		e.status = SharedRecipeReportStatus.PENDING;
		return e;
	}
}
