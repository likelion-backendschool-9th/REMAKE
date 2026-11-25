package com.remake.mealplan.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.mealplan.application.port.in.DeleteMealSlotUseCase;
import com.remake.mealplan.application.port.in.ReplaceMealSlotUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "식단표")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/meal-plan-slots")
@RequiredArgsConstructor
public class MealPlanSlotController {

	private final DeleteMealSlotUseCase deleteMealSlotUseCase;
	private final ReplaceMealSlotUseCase replaceMealSlotUseCase;

	@Operation(summary = "식단 칸 삭제", description = """
			오늘부터의 내 식단 칸 하나를 지웁니다. 칸의 레시피와 장바구니 항목은 남습니다.
			거절: 지난 날짜 PAST_DATE_NOT_ALLOWED, 없거나 내 칸이 아님(덮어쓰기로 지워진 칸 포함) SLOT_NOT_FOUND.""")
	@DeleteMapping("/{slotId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@LoginUserId Long userId, @PathVariable Long slotId) {
		deleteMealSlotUseCase.delete(userId, slotId);
	}

	@Operation(summary = "식단 칸 레시피 교체", description = """
			오늘부터의 내 식단 칸 하나의 레시피를 새로 만들어 바꿉니다. 그 식단표의 목적·인원과 냉장고 재료로 만들고(이 칸이 쓰던 재료는 다시 씀),
			성공할 때만 레시피 하루 횟수를 1회 씁니다(레시피 생성과 같은 횟수, 식단표 월 한도는 안 씀). 요리한 칸이면 요리 완료 표시가 지워집니다.
			AI 호출 때문에 최대 약 55초 걸릴 수 있습니다. 실패하면 기존 레시피와 횟수는 그대로입니다.
			거절: 지난 날짜 PAST_DATE_NOT_ALLOWED, 없거나 내 칸이 아님 SLOT_NOT_FOUND, 횟수 없음 GENERATION_LIMIT_EXCEEDED,
			쓸 수 있는 냉장고 재료 없음 MEAL_PLAN_NO_INGREDIENTS(422), AI 실패 AI_UPSTREAM_ERROR·AI_TIMEOUT·AI_INVALID_RESPONSE.""")
	@PostMapping("/{slotId}/replace")
	public ReplaceMealSlotResponse replace(@LoginUserId Long userId, @PathVariable Long slotId) {
		return ReplaceMealSlotResponse.from(replaceMealSlotUseCase.replace(userId, slotId));
	}
}
