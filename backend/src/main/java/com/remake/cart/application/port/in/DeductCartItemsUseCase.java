package com.remake.cart.application.port.in;

import java.util.List;

/**
 * #57 영수증 선택 차감 계약. 호출자의 영수증·냉장고 등록 트랜잭션에 참여한다.
 * 영수증 쪽은 품목당 대상 0~1개, 양수 차감량, 실제 등록량 이내인지 먼저 검증한다.
 * 장바구니 쪽은 본인 항목·최종 재료명·동일 단위와 대상별 합산 차감량을 검증한다.
 * 검증 실패는 BusinessException으로 전달하여 호출자의 등록도 함께 롤백한다.
 */
public interface DeductCartItemsUseCase {

	/**
	 * 반환값은 잔량이 0이 되어 실제 삭제된 행 수(cartRemovedCount)이다. 수량 합이나 요청 수가 아니다.
	 * 빈 목록은 0. 같은 행을 여러 번 요청하면 수량을 합산하고 삭제 행은 한 번만 센다.
	 * 없는 항목·타인 항목은 CART_ITEM_NOT_FOUND, 이름·단위·수량 오류는 INVALID_INPUT,
	 * 합산량이 잔량보다 많으면 INSUFFICIENT_QUANTITY(details.remaining)이다. 단위 환산은 하지 않는다.
	 */
	int deduct(Long userId, List<CartDeductionCommand> deductions);
}
