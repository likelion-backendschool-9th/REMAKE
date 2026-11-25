package com.remake.recipe.application.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.recipe.application.port.in.GetCartRecipeTitlesUseCase;
import com.remake.recipe.application.port.out.LoadRecipePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CartRecipeTitleQueryService implements GetCartRecipeTitlesUseCase {
	private final LoadRecipePort recipes;

	@Override
	@Transactional(readOnly = true)
	public Map<Long, String> getTitles(Collection<Long> recipeIds) {
		if (recipeIds.isEmpty()) {
			return Map.of();
		}
		return recipes.loadHeaders(recipeIds).values().stream()
				.collect(Collectors.toMap(h -> h.recipeId(), h -> h.title()));
	}
}
