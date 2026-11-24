package com.remake.share.adapter.out.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.out.SaveSharedRecipeReportPort;
import com.remake.share.domain.SharedRecipeReportReason;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SharedRecipeReportPersistenceAdapter implements SaveSharedRecipeReportPort {
	private final SharedRecipeReportJpaRepository repository;
	@Override
	public void save(Long userId, Long sharedRecipeId, SharedRecipeReportReason reason) {
		try {
			repository.saveAndFlush(SharedRecipeReportJpaEntity.create(userId, sharedRecipeId, reason));
		} catch (DataIntegrityViolationException failure) {
			if (failure.getCause() instanceof org.hibernate.exception.ConstraintViolationException violation
					&& "uk_shared_recipe_reports_reporter_post".equals(violation.getConstraintName())) {
				throw new BusinessException(ErrorCode.REPORT_ALREADY_EXISTS);
			}
			throw failure;
		}
	}
}
