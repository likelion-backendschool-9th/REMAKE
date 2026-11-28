package com.remake.cart.application.port.in;

import java.util.List;

/**
 * #57 영수증 장바구니 차감. 호출자의 영수증·냉장고 등록 트랜잭션에 참여한다.
 * 신규 연결은 자동 차감을 사용하고, 기존 선택 차감은 #315 전환 전 호환용으로 유지한다.
 * 검증·저장 실패는 예외로 전달하여 호출자의 등록도 함께 롤백한다.
 */
public interface DeductCartItemsUseCase {

	/**
	 * 이름·단위별 등록량을 합산하고 본인 장바구니를 ID 오름차순으로 잔량까지만 차감한다.
	 * 불일치·초과분은 무시하며 단위 환산은 하지 않는다. 빈 목록은 0이다.
	 * 반환값은 실제 삭제 행 수이며, 호출자의 등록 트랜잭션에 참여한다.
	 * 잘못된 입력은 INVALID_INPUT이다. 영수증 API 연결은 #315에서 전환한다.
	 */
	int deductAutomatically(Long userId, List<AutomaticCartDeductionCommand> deductions);

	/**
	 * #315 전환 전 호출자 호환용 선택 차감. 신규 연결은 deductAutomatically를 사용한다.
	 * 영수증 쪽은 품목당 대상 0~1개·양수 차감량·실제 등록량 상한을 검증한다.
	 * 반환값은 잔량이 0이 되어 실제 삭제된 행 수(cartRemovedCount)이다. 수량 합이나 요청 수가 아니다.
	 * 빈 목록은 0. 같은 행을 여러 번 요청하면 수량을 합산하고 삭제 행은 한 번만 센다.
	 * 없는 항목·타인 항목은 CART_ITEM_NOT_FOUND, 이름·단위·수량 오류는 INVALID_INPUT,
	 * 합산량이 잔량보다 많으면 INSUFFICIENT_QUANTITY(details.remaining)이다. 단위 환산은 하지 않는다.
	 */
	int deduct(Long userId, List<CartDeductionCommand> deductions);
}
