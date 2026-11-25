package com.remake.share.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.out.LoadSharedRecipePort;
import com.remake.share.application.port.out.SaveSharedRecipePort;
import com.remake.share.domain.SharedRecipe;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SharedRecipePersistenceAdapter implements SaveSharedRecipePort, LoadSharedRecipePort {
	private final SharedRecipeJpaRepository repository;

	@Override
	public Long save(Long recipeId, String title, String imageKey) {
		try {
			return repository.saveAndFlush(SharedRecipeJpaEntity.create(recipeId, title, imageKey)).getId();
		} catch (DataIntegrityViolationException failure) {
			if (failure.getCause() instanceof org.hibernate.exception.ConstraintViolationException violation
					&& "uk_shared_recipes_recipe_week".equals(violation.getConstraintName())) {
				throw new BusinessException(ErrorCode.ALREADY_SHARED);
			}
			throw failure;
		}
	}
	@Override
	public boolean existsInWeek(Long recipeId, LocalDateTime start, LocalDateTime end) {
		return repository.existsByRecipeIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(recipeId, start, end);
	}
	@Override
	public Optional<SharedRecipe> findById(Long id) {
		return repository.findById(id).map(SharedRecipeJpaEntity::toDomain);
	}
}
