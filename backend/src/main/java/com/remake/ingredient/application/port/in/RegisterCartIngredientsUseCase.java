package com.remake.ingredient.application.port.in;

import java.util.List;

/** 장바구니 이동 전용 등록. 서버가 수량을 보존해 확정한 단위를 사용하고 E22의 출처를 cart로 남긴다. */
public interface RegisterCartIngredientsUseCase {
	List<Long> registerCartIngredients(List<RegisterIngredientCommand> commands);
}
