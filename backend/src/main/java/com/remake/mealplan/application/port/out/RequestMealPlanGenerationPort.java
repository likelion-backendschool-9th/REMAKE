package com.remake.mealplan.application.port.out;

import com.remake.mealplan.application.port.in.MealPlanGenerationJob;

public interface RequestMealPlanGenerationPort {

	/**
	 * 서버 작업을 예약한다. 지금 트랜잭션이 커밋된 뒤에 시작하고, 롤백되면 시작하지 않는다.
	 * 작업은 요청 스레드가 아닌 작업 실행기에서 돈다.
	 */
	void request(MealPlanGenerationJob job);
}
