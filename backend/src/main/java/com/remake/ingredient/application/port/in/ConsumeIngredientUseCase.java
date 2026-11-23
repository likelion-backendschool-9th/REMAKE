package com.remake.ingredient.application.port.in;

/**
 * 재료 사용·폐기. 냉장고의 "사용했어요/버렸어요"와 요리 후 재료 차감(#77)이 같이 쓴다.
 * 호출한 쪽의 트랜잭션에 합류하므로 요리 후 차감과 함께 성공하거나 함께 롤백된다.
 * ingredient_used·ingredient_discarded 이벤트는 이 경계에서만 발행한다(호출한 쪽은 다시 발행하지 않는다).
 */
public interface ConsumeIngredientUseCase {

	/**
	 * 사용자 본인의 재료에서 amount만큼 뺀다. 남은 양이 0이 되면 재료 행을 지우고(기록은 이름·단위와 함께 남는다)
	 * deleted가 true인 결과를 돌려준다.
	 * 없거나 다른 사용자의 재료(이미 다 써서 지운 것 포함)면 INGREDIENT_NOT_FOUND, 남은 양보다 많으면
	 * INSUFFICIENT_QUANTITY(details.remaining), 수량 형식이 틀리면 INVALID_INPUT.
	 */
	ConsumeIngredientResult consumeIngredient(ConsumeIngredientCommand command);
}
