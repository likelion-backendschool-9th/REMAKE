package com.remake.cart.application.port.in;

import java.util.List;

public interface DeleteCartItemsUseCase {
	/** 본인 항목만 일괄 삭제. 하나라도 없으면 CART_ITEM_NOT_FOUND, 전체 유지. */
	void deleteItems(Long userId, List<Long> ids);
}
