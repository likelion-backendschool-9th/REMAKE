package com.remake.mealplan.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.mealplan.application.port.in.GenerateMealPlanUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "식단표")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {

	private final GenerateMealPlanUseCase generateMealPlanUseCase;

	@Operation(summary = "식단표 생성", description = """
			조건을 확인하고 바로 202로 답합니다. 생성은 서버 작업이 하고, 완성·실패는 앱 안 알림으로 알립니다.
			거절: 지난 시작일 PAST_DATE_NOT_ALLOWED, 생성 중 MEAL_PLAN_IN_PROGRESS, 이번 달 횟수 소진 MEAL_PLAN_LIMIT_EXCEEDED,
			겹침 MEAL_PLAN_OVERLAP(details.overlappingDates, overwrite: true로 다시 보내면 덮어씀).""")
	@PostMapping("/generate")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public GenerateMealPlanResponse generate(@LoginUserId Long userId,
			@Valid @RequestBody GenerateMealPlanRequest request) {
		return GenerateMealPlanResponse.from(generateMealPlanUseCase.generate(request.toCommand(userId)));
	}
}
