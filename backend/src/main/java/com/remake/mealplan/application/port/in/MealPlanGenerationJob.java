package com.remake.mealplan.application.port.in;

import java.time.Instant;

/**
 * 접수한 식단표의 서버 작업 하나.
 *
 * @param maxDays    접수 때 요금제의 최대 일수(일반 5, 구독 7). 재료가 모자라면 이보다 적게 만든다
 * @param overwrite  겹치는 날의 기존 칸을 바꿀지(접수 때 겹침을 확인했으므로, 덮어쓰기가 아니면 겹치는 칸이 없다)
 * @param acceptedAt 접수 시각. E53·E54의 duration_ms는 여기서부터 잰다
 */
public record MealPlanGenerationJob(Long mealPlanId, Long userId, int maxDays, boolean overwrite, Instant acceptedAt) {
}
