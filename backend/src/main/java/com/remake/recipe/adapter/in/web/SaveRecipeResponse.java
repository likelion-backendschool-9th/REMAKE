package com.remake.recipe.adapter.in.web;

/** API 34 응답. cartAddedCount는 장바구니에 새로 담은 행 수 */
public record SaveRecipeResponse(boolean saved, int cartAddedCount) {
}
