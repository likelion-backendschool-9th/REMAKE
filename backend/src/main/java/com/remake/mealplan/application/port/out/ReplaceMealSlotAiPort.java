package com.remake.mealplan.application.port.out;

import java.util.List;

/**
 * 식단 칸 교체(API 42)의 새 레시피를 AI로 만든다. 지시문은 식단표 생성과 같고, 교체 규칙(피할 요리 이름)만 덧붙인다.
 * 사용자가 응답을 기다리므로 호출 한 번은 25초 안이다(#50: 25초 × 2 + 쉬는 시간 < Nginx 60초).
 */
public interface ReplaceMealSlotAiPort {

	/**
	 * request.schedule()의 칸 하나(하루·끼니 하나)를 만든다. 모양만 읽고 칸 검사는 부르는 쪽이 한다.
	 * 실패하면 BusinessException(AI_UPSTREAM_ERROR, AI_TIMEOUT, AI_INVALID_RESPONSE)을 던진다.
	 * 공급자 과부하(503)도 쉬며 다시 부르지 않고 AI_UPSTREAM_ERROR로 던진다.
	 */
	List<GeneratedMealSlot> replace(MealPlanAiRequest request);
}
