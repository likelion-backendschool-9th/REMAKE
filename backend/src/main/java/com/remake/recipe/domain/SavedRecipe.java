package com.remake.recipe.domain;

import java.time.LocalDateTime;

/**
 * 내 저장 목록의 한 건(saved_recipes). 공유 레시피를 저장해도 원본 recipeId를 가리킨다(사본 없음).
 *
 * @param favorite 즐겨찾기(별)
 */
public record SavedRecipe(Long recipeId, boolean favorite, LocalDateTime savedAt) {
}
