package com.remake.mealplan.application.port.out;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

/**
 * AI 공급자가 과부하로 잠시 거절했다(Gemini 503). 오류 코드는 AI_UPSTREAM_ERROR 그대로이고,
 * 서버 작업은 이 경우만 쉬는 시간을 늘려 가며 더 시도한다(11-24 실측: 503이 잦고 50초 넘게 이어지기도 했지만 잠시 뒤 성공).
 */
public class MealPlanAiBusyException extends BusinessException {

	public MealPlanAiBusyException() {
		super(ErrorCode.AI_UPSTREAM_ERROR);
	}
}
