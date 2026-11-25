package com.remake.recipe.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.remake.recipe.application.port.out.SavedRecipePort;
import com.remake.recipe.domain.SavedRecipe;
import com.remake.recipe.domain.SavedRecipeSort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SavedRecipePersistenceAdapter implements SavedRecipePort {

	/** availableNames가 없을 때 not in에 넣는 값. 빈 목록을 넘기지 않기 위한 것이라 쓰이지 않는다 */
	private static final List<String> NO_NAMES = List.of("");

	private final SavedRecipeJpaRepository repository;

	@Override
	public Optional<SavedRecipe> find(Long userId, Long recipeId) {
		return repository.findByUserIdAndRecipeId(userId, recipeId).map(SavedRecipeJpaEntity::toDomain);
	}

	@Override
	public boolean insertIfAbsent(Long userId, Long recipeId, LocalDateTime savedAt) {
		return repository.insertIfAbsent(userId, recipeId, savedAt) > 0;
	}

	@Override
	public boolean insertSharedIfAbsent(Long userId, Long recipeId, Long sharedRecipeId, LocalDateTime savedAt) {
		return repository.insertSharedIfAbsent(userId, recipeId, sharedRecipeId, savedAt) > 0;
	}

	@Override
	public void delete(Long userId, Long recipeId) {
		repository.deleteByUserIdAndRecipeId(userId, recipeId);
	}

	@Override
	public void deleteAllByUserId(Long userId) {
		repository.deleteAllByUserId(userId);
	}

	@Override
	public void updateFavorite(Long userId, Long recipeId, boolean favorite) {
		repository.updateFavorite(userId, recipeId, favorite);
	}

	@Override
	public long countByUserId(Long userId) {
		return repository.countByUserId(userId);
	}

	@Override
	public Page<SavedRecipe> findPage(Long userId, boolean favoriteOnly, Collection<String> availableNames,
			SavedRecipeSort sort, int page, int size) {
		Sort.Direction direction = sort == SavedRecipeSort.SAVED_ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
		// 같은 시각에 저장한 것은 id로 순서를 고정한다
		PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, "savedAt").and(Sort.by(direction, "id")));
		boolean availableOnly = availableNames != null;
		Collection<String> names = availableOnly && !availableNames.isEmpty() ? availableNames : NO_NAMES;
		return repository.findPage(userId, favoriteOnly, availableOnly, names, pageable)
				.map(SavedRecipeJpaEntity::toDomain);
	}

	@Override
	public Map<Long, Boolean> findFavorites(Long userId, Collection<Long> recipeIds) {
		return repository.findByUserIdAndRecipeIdIn(userId, recipeIds).stream()
				.collect(Collectors.toMap(SavedRecipeJpaEntity::getRecipeId, SavedRecipeJpaEntity::isFavorite));
	}
}
