package com.remake.ingredient.application.port.in;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;

/**
 * 재료명으로 단위를 정한다. 재료 직접 추가·수정과 영수증 등록이 이 UseCase를 쓴다.
 * 장바구니 이동은 #89에서 장바구니 담당 유리(@ureeStar)가 2026-11-24 결정한 수량 보존 예외를 적용한다.
 * 기존 단위를 유지하고 계량 단위만 ml로 환산하며, 결정 근거와 냉장고 영향 확인 요청은 #145에 기록한다.
 * 사용자는 단위를 고르지 않고 서버가 정한다.
 */
public interface ResolveIngredientUnitUseCase {

	/**
	 * 순서는 (1) 사전 이름과 정확히 같으면 그 단위, (2) 재료명 안에 들어 있는 같은 카테고리의 사전 이름 중 가장 긴 것의 단위,
	 * (3) 그래도 없으면 EA이다. categoryCode가 null이면 2번을 건너뛴다. 이름이 비어 있으면 INVALID_INPUT이다.
	 */
	IngredientUnit resolveUnit(String name, IngredientCategoryCode categoryCode);
}
