package com.remake.quota.application.port.in;

import com.remake.quota.domain.RecipeQuota;

/**
 * 레시피 생성 횟수 확인·차감. 빠른·커스텀 생성, 다시 받기, 식단 칸 교체가 같은 하루 횟수를 쓴다.
 * <ol>
 * <li>AI를 부르기 전에 {@link #requireRecipeQuota}로 남은 횟수를 확인한다(잠그지 않음)</li>
 * <li>AI 결과가 검증을 통과하면 결과를 저장하는 트랜잭션 안에서 {@link #consumeRecipeQuota}를 부른다</li>
 * </ol>
 * 실패(AI 오류·시간 초과·형식 오류·NO_INGREDIENTS)는 차감하지 않으므로 consume을 부르지 않는다.
 */
public interface ConsumeRecipeQuotaUseCase {

	/** 남은 횟수가 없으면 GENERATION_LIMIT_EXCEEDED(details: remaining = 0, resetsAt)를 던진다. 있으면 현재 횟수를 돌려준다. */
	RecipeQuota requireRecipeQuota(Long userId);

	/**
	 * 호출한 쪽의 트랜잭션에 참여해(REQUIRED) 회원 단위로 잠그고 남은 횟수를 다시 센 뒤 usage_logs에 한 행을 넣는다.
	 * 같은 회원의 요청이 동시에 와도 한도를 넘지 않는다. 그 사이 횟수를 다 썼으면 GENERATION_LIMIT_EXCEEDED를 던지고,
	 * 호출한 쪽 트랜잭션이 롤백되어 결과도 저장되지 않는다. 차감 뒤의 횟수를 돌려준다.
	 */
	RecipeQuota consumeRecipeQuota(Long userId);
}
