package com.remake.ingredient.application.port.in;

import java.util.List;

/**
 * 재료 등록. 호출한 쪽의 트랜잭션에 합류하므로, 영수증 확정은 영수증·품목을 저장한 같은 트랜잭션 안에서 부른다.
 * 실패는 BusinessException(INVALID_INPUT, INGREDIENT_CATEGORY_NOT_FOUND)으로 던지고, 호출한 쪽이 롤백한다.
 */
public interface RegisterIngredientUseCase {

	/** 한 건 등록(직접 추가 API). 만든 재료의 id를 돌려준다. */
	Long registerIngredient(RegisterIngredientCommand command);

	/** 여러 건 등록(영수증 확정). 하나라도 실패하면 예외를 던진다. 만든 재료의 id를 입력 순서대로 돌려준다. */
	List<Long> registerIngredients(List<RegisterIngredientCommand> commands);
}
