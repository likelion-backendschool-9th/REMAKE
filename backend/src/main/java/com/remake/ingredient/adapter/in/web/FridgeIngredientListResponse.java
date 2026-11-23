package com.remake.ingredient.adapter.in.web;

import java.util.List;

/** 냉장고 목록 응답(API 12). 페이징 없이 전부 `items`에 담는다. 재료가 없으면 빈 배열이다. */
public record FridgeIngredientListResponse(List<FridgeIngredientResponse> items) {
}
