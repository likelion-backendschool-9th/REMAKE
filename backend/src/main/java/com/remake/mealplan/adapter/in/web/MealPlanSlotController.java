package com.remake.mealplan.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.mealplan.application.port.in.DeleteMealSlotUseCase;

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

	@Operation(summary = "식단 칸 삭제", description = """
			오늘부터의 내 식단 칸 하나를 지웁니다. 칸의 레시피와 장바구니 항목은 남습니다.
			거절: 지난 날짜 PAST_DATE_NOT_ALLOWED, 없거나 내 칸이 아님(덮어쓰기로 지워진 칸 포함) SLOT_NOT_FOUND.""")
	@DeleteMapping("/{slotId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@LoginUserId Long userId, @PathVariable Long slotId) {
		deleteMealSlotUseCase.delete(userId, slotId);
	}
}
