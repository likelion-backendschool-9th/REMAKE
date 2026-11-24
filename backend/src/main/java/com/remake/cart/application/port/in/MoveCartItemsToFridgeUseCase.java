package com.remake.cart.application.port.in;

import java.util.List;

public interface MoveCartItemsToFridgeUseCase {
	/** 냉장고 등록과 선택 행 삭제를 한 트랜잭션으로 처리하고 실제 등록 행 수를 반환한다. */
	int move(Long userId, List<Long> ids);
}
