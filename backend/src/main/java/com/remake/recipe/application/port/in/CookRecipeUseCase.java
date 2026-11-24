package com.remake.recipe.application.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.remake.ingredient.domain.IngredientUnit;

/**
 * 요리 후 재료 차감(API 31·32, #77). 내가 만들었거나 저장한 레시피만 된다(그 밖은 RECIPE_NOT_FOUND).
 */
public interface CookRecipeUseCase {

	/**
	 * 차감할 내 냉장고 재료 후보. 레시피 재료 순서대로, 같은 이름의 소비기한이 지나지 않은 냉장고 재료 중 소비기한이 가장 빠른 것 하나와 짝짓는다.
	 * 물·꼬집·약간·양 없는 재료와 냉장고에 없는 재료는 빠진다.
	 *
	 * @param amount          냉장고 단위로 바꾼 차감 예정량(남은 양을 넘지 않음). 단위 종류가 달라 바꿀 수 없으면(양파 1개 ↔ 300g,
	 *                        팩 ↔ g, #145) null이고, 사용자가 직접 정한다
	 * @param remainingAmount 지금 남은 양
	 */
	record PreviewItem(Long ingredientId, String name, String imageUrl, BigDecimal amount, IngredientUnit unit,
			BigDecimal remainingAmount) {
	}

	/** 체크한 재료와 사용자가 정한 차감량(냉장고 단위). */
	record CookItem(Long ingredientId, BigDecimal amount) {
	}

	List<PreviewItem> preview(Long userId, Long recipeId);

	/**
	 * 체크한 재료만 USED로 차감하고 요리를 완료한다. 하나라도 실패하면 모두 되돌린다. 아무것도 체크하지 않아도 요리 완료는 된다.
	 * <ul>
	 * <li>내 재료가 아니거나(이미 다 써서 지운 것 포함) 양이 0 이하이거나 같은 재료가 두 번 오면 COOK_ITEM_INVALID</li>
	 * <li>남은 양보다 많으면 INSUFFICIENT_QUANTITY</li>
	 * <li>mealPlanSlotId는 식단 칸에서 열었을 때만 온다. 오늘 칸이면 요리 완료로 표시한다</li>
	 * </ul>
	 *
	 * @return 차감한 재료 수
	 */
	int cook(Long userId, Long recipeId, List<CookItem> items, Long mealPlanSlotId);
}
