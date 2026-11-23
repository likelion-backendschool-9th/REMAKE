package com.remake.recipe.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.remake.recipe.domain.CustomOptions;
import com.remake.recipe.domain.RecipeSource;

/**
 * AI에 레시피 생성을 요청할 때 넘기는 값. 고른 옵션(id·enum)과 DB 재료 정보만 담는다.
 * 사용자가 직접 쓴 글은 넣지 않는다(AGENTS.md 규칙 2). 재료 이름은 냉장고 재료 이름이다.
 *
 * @param source                QUICK 또는 CUSTOM
 * @param servings              인분(빠른 생성은 가구원 수, 커스텀은 고른 인원)
 * @param supplementEnabled     재료 보충. 켜져 있으면 냉장고에 없는 재료도 넣을 수 있다
 * @param fridgeIngredients     쓸 수 있는 내 냉장고 재료. 소비기한이 빠른 순
 * @param customOptions         커스텀 생성의 선택값. 빠른 생성은 null
 * @param excludedAllergenCodes 빼야 할 알레르기 칩 코드. 없으면 빈 목록
 */
public record RecipeAiRequest(
		RecipeSource source,
		int servings,
		boolean supplementEnabled,
		List<FridgeItem> fridgeIngredients,
		CustomOptions customOptions,
		List<String> excludedAllergenCodes) {

	public RecipeAiRequest {
		fridgeIngredients = fridgeIngredients == null ? List.of() : List.copyOf(fridgeIngredients);
		excludedAllergenCodes = excludedAllergenCodes == null ? List.of() : List.copyOf(excludedAllergenCodes);
	}

	/**
	 * 냉장고 재료 한 건.
	 *
	 * @param unit         단위 코드(G, EA 등)
	 * @param expiringSoon 소비기한 임박(우선 사용 대상)
	 */
	public record FridgeItem(Long ingredientId, String name, BigDecimal amount, String unit,
			LocalDate expirationDate, boolean expiringSoon) {
	}
}
