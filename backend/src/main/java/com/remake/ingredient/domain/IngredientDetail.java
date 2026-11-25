package com.remake.ingredient.domain;

/**
 * 재료 상세·수정 결과(API 13·15·16). 냉장고 한 건에 "단위를 다시 정하는 재료인지"를 더했다.
 * unitFollowsName은 지금 단위가 지금 이름·카테고리의 사전 단위와 같아, 이름이나 카테고리를 고치면 단위도 다시 정해지는지다(#233).
 * 장바구니·영수증 예외로 사전과 다른 단위를 가진 재료는 false이고 수정해도 단위가 그대로다.
 * 목록(API 12)과 다른 모듈이 받는 {@link FridgeIngredient}에는 넣지 않는다(목록에서는 계산하지 않아 값이 거짓일 수 있다).
 */
public record IngredientDetail(FridgeIngredient fridgeIngredient, boolean unitFollowsName) {
}
