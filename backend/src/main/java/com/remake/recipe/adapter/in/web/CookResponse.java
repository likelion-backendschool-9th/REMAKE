package com.remake.recipe.adapter.in.web;

/** 요리 완료(API 32) 응답. 차감한 재료 수. */
public record CookResponse(int deductedCount) {
}
