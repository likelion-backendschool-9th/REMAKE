package com.remake.cart.application.port.in;

import java.util.Map;

/**
 * 식단에서 레시피가 빠질 때(칸 삭제 API 41, 칸 교체 API 42, 덮어쓰기 API 38) 식단표가 부른다(#91).
 * 그 칸에서 그 레시피로 담은 행(사용자 + meal_plan_slot_id + recipe_id)을 고른 뒤:
 * <ul>
 * <li>담은 뒤 손대지 않은 행(updated_at = created_at)은 지운다. 안 먹을 요리의 재료를 사지 않게 하려는 것이다</li>
 * <li>수량을 고쳤거나 영수증으로 일부 차감된 행, 사용자가 저장(북마크)해 둔 레시피의 행은 남기고 칸 연결만 비운다</li>
 * </ul>
 * 고른 행은 id 순서로 잠근 뒤 판단해 수량 수정·영수증 차감과 겹쳐도 고친 수량이 지워지지 않는다.
 * 칸을 지우거나 칸의 recipe_id를 바꾸기 전에 불러야 한다(칸을 먼저 지우면 SET NULL로 어느 칸 행인지 알 수 없다).
 * 식단표 레시피는 칸마다 따로 만들어지므로 같은 recipe_id를 쓰는 다른 칸은 없다.
 * 호출자의 트랜잭션에 참여한다. 이벤트는 남기지 않는다.
 */
public interface CleanUpMealPlanSlotCartItemsUseCase {

	/**
	 * @param slotRecipeIds 빠지는 칸 id → 그 칸에서 빠지는 레시피 id
	 * @return 지운 행 수
	 */
	int cleanUp(Long userId, Map<Long, Long> slotRecipeIds);
}
