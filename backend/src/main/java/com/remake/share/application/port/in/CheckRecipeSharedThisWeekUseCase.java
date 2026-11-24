package com.remake.share.application.port.in;

/** 레시피 상세(API 30)의 shareable용. 이번 주(KST 월요일 시작)에 이 원본 레시피를 이미 공유했는지. */
public interface CheckRecipeSharedThisWeekUseCase {

	boolean isSharedThisWeek(Long recipeId);
}
