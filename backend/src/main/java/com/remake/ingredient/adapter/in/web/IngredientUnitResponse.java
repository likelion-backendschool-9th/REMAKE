package com.remake.ingredient.adapter.in.web;

import com.remake.ingredient.domain.IngredientUnit;

/** 재료명에 맞는 단위 응답(API 14). enum은 이름 그대로(BUNCH) 나간다. */
public record IngredientUnitResponse(IngredientUnit unit) {
}
