package com.remake.recipe.adapter.out.persistence;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.remake.recipe.application.port.out.LoadRecipePort;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;

import lombok.RequiredArgsConstructor;

/** 레시피 읽기·저장 Port 구현. 레시피·재료·단계 세 테이블을 합쳐 도메인 객체로 주고받는다. 엔티티는 이 패키지 밖으로 나가지 않는다. */
@Component
@RequiredArgsConstructor
class RecipePersistenceAdapter implements SaveRecipePort, LoadRecipePort {

	private final RecipeJpaRepository recipeRepository;
	private final RecipeIngredientJpaRepository ingredientRepository;
	private final RecipeStepJpaRepository stepRepository;

	@Override
	public Recipe save(Recipe recipe) {
		if (recipe.id() != null) {
			throw new IllegalArgumentException("레시피는 만든 뒤 고치지 않습니다.");
		}
		RecipeJpaEntity saved = recipeRepository.save(RecipeJpaEntity.from(recipe));
		Long recipeId = saved.getId();

		List<RecipeIngredient> ingredients = recipe.ingredients();
		List<RecipeIngredientJpaEntity> ingredientEntities = ingredientRepository.saveAll(IntStream
				.range(0, ingredients.size())
				.mapToObj(i -> RecipeIngredientJpaEntity.of(recipeId, ingredients.get(i), i))
				.toList());

		List<String> steps = recipe.steps();
		List<RecipeStepJpaEntity> stepEntities = stepRepository.saveAll(IntStream.range(0, steps.size())
				.mapToObj(i -> RecipeStepJpaEntity.of(recipeId, i + 1, steps.get(i)))
				.toList());

		return toDomain(saved, ingredientEntities, stepEntities);
	}

	@Override
	public Optional<Recipe> loadByIdAndUserId(Long recipeId, Long userId) {
		return recipeRepository.findByIdAndUserId(recipeId, userId)
				.map(entity -> toDomain(entity, ingredientRepository.findByRecipeIdOrderBySortOrderAsc(recipeId),
						stepRepository.findByRecipeIdOrderByStepNoAsc(recipeId)));
	}

	@Override
	public Map<Long, List<RecipeIngredient>> loadIngredientsByRecipeIds(Collection<Long> recipeIds, Long userId) {
		if (recipeIds.isEmpty()) {
			return Map.of();
		}
		return ingredientRepository.findAllOfUserRecipes(recipeIds, userId).stream()
				.collect(Collectors.groupingBy(RecipeIngredientJpaEntity::getRecipeId, LinkedHashMap::new,
						Collectors.mapping(RecipeIngredientJpaEntity::toDomain, Collectors.toList())));
	}

	private static Recipe toDomain(RecipeJpaEntity entity, List<RecipeIngredientJpaEntity> ingredients,
			List<RecipeStepJpaEntity> steps) {
		return Recipe.restore(entity.getId(), entity.getUserId(), entity.getSource(), entity.getTitle(),
				entity.getServings(), entity.getCookMinutes(), entity.getTips(), entity.getCustomOptions(),
				ingredients.stream().map(RecipeIngredientJpaEntity::toDomain).toList(),
				steps.stream().map(RecipeStepJpaEntity::getContent).toList(), entity.getCreatedAt());
	}
}
