package com.remake.cart.application.port.in;

import java.util.List;

import com.remake.cart.domain.CartSource;

/**
 * 서버 내부 공통 담기. 호출자는 현재 냉장고 기준의 부족 재료만 전달한다. 수동 담기 API는 없다.
 * 호출자의 저장·식단 생성 트랜잭션에 참여하고 실제 추가 행 수를 반환한다.
 * userId+원본 recipeId+공백을 정리한 정확한 재료명+단위로 중복을 막으며 기존 수량·슬롯은 유지한다.
 * MEAL_PLAN은 이번 호출의 같은 키 수량을 먼저 합산한다. 기존 행에는 합산량을 누적하지 않는다.
 * E62는 이 UseCase가 실제 추가가 있을 때만 한 번 발행한다. 호출자가 중복 발행하지 않는다.
 */
public interface AddCartItemsUseCase {
	int add(Long userId, CartSource source, List<CartAdditionCommand> items);
}
