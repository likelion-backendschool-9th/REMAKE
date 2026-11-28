package com.remake.receipt.application.port.in;

import java.math.BigDecimal;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

public interface UpdateManualIngredientExpenseUseCase {

	/** 재료 수정 트랜잭션에서 호출한다. 지출 행이 없으면 건너뛰고 최초 등록일을 유지한다. */
	void update(Command command);

	/** 변경되지 않은 필드는 null이다. priceChanged가 true일 때만 가격을 반영하며 null은 가격 삭제다. */
	record Command(Long ingredientId, String name, Long categoryId, StorageLocation storage,
			BigDecimal amount, IngredientUnit unit, boolean priceChanged, Long price) {

		public boolean hasChanges() {
			return name != null || categoryId != null || storage != null || amount != null || unit != null || priceChanged;
		}
	}
}
