package com.remake.mealplan.adapter.in.web;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import com.remake.mealplan.application.port.in.GenerateMealPlanCommand;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 식단표 생성 요청(API 38). 고른 값만 받는다(사용자가 쓴 글 없음).
 * 형식·범위가 틀리면 INVALID_INPUT(fieldErrors), 지난 시작일은 서비스가 PAST_DATE_NOT_ALLOWED로 답한다.
 */
public record GenerateMealPlanRequest(
		@NotNull @Schema(description = "시작일(오늘부터)", example = "2026-11-24") LocalDate startDate,
		@NotNull @Schema(description = "식단 목적") MealPurpose purpose,
		@NotEmpty @Size(max = 3) @Schema(description = "넣을 끼니(1~3개, 중복 없음)") List<@NotNull MealType> mealTypes,
		@NotNull @Min(1) @Max(50) @Schema(description = "식사 인원(1~50, 가구원 수·레시피 인원과 같은 범위)") Integer servings,
		@Schema(nullable = true, description = "겹치는 날을 덮어쓸지. 보내지 않으면 false(겹치면 MEAL_PLAN_OVERLAP)") Boolean overwrite) {

	@AssertTrue(message = "끼니는 중복 없이 골라야 합니다.")
	@Schema(hidden = true)
	public boolean isMealTypesUnique() {
		return mealTypes == null || mealTypes.stream().distinct().count() == mealTypes.size();
	}

	GenerateMealPlanCommand toCommand(Long userId) {
		return new GenerateMealPlanCommand(userId, startDate, purpose, EnumSet.copyOf(mealTypes), servings,
				Boolean.TRUE.equals(overwrite));
	}
}
