package com.remake.receipt.application.port.in;

public interface UpdateManualIngredientExpensePriceUseCase {

	/**
	 * 직접 등록 지출 행의 가격을 바꾼다. {@code price}가 null이면 소비 패턴 합계에서 빠진다.
	 * 행이 없으면 아무 것도 하지 않는다. 재료 수정과 같은 트랜잭션에서만 호출한다.
	 */
	void updatePrice(Long ingredientId, Long price);
}
