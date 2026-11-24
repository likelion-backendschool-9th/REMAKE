package com.remake.receipt.application.service;

import java.math.BigDecimal;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.error.InvalidFieldException;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Item;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.ItemRequest;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Volume;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.VolumeScope;

final class ReceiptQuantity {

	private ReceiptQuantity() {
	}

	static Item resolve(ItemRequest request, IngredientUnit unit, int itemIndex) {
		BigDecimal amount = request.amount();
		BigDecimal purchaseQuantity = request.purchaseQuantity();
		Volume volume = request.volume();

		int group = measurementGroup(unit);
		int volumeGroup = volume == null ? 0 : measurementGroup(volume.unit());
		if (volume != null && volumeGroup == 0) {
			throw new InvalidFieldException("items[" + itemIndex + "].volume.unit", "용량에는 무게 또는 부피 단위를 사용해 주세요.");
		}
		if (group != 0 && purchaseQuantity != null && group != volumeGroup) {
			unit = IngredientUnit.PACK;
		}
		if (amount == null) {
			if (measurementGroup(unit) == 0) {
				amount = purchaseQuantity;
			} else if (volume != null && group == volumeGroup
					&& (volume.scope() == VolumeScope.TOTAL || purchaseQuantity != null)) {
				amount = volume.amount().scaleByPowerOfTen(unitScale(volume.unit()) - unitScale(unit));
				if (volume.scope() == VolumeScope.PER_PACK) {
					amount = amount.multiply(purchaseQuantity);
				}
			}
		}

		if (amount == null) {
			throw new InvalidFieldException("items[" + itemIndex + "].purchaseQuantity", "구매 수량을 확인해 주세요.");
		}
		if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 3
				|| amount.compareTo(new BigDecimal("10000000")) >= 0) {
			throw new InvalidFieldException("items[" + itemIndex + "].amount", "등록할 품목 수량을 확인해 주세요.");
		}
		var deductions = request.cartDeductions();
		if (deductions.size() > 1) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "품목마다 장바구니 항목을 하나만 선택해 주세요.");
		}
		if (!deductions.isEmpty() && deductions.getFirst().amount().compareTo(amount) > 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "장바구니 차감 수량이 등록 수량을 초과합니다.");
		}
		return new Item(request.name().strip(), request.categoryCode(), amount, unit, request.price(), request.storage(),
				request.expirationDate());
	}

	private static int measurementGroup(IngredientUnit unit) {
		return switch (unit) {
			case G, KG -> 1;
			case ML, L -> 2;
			default -> 0;
		};
	}

	private static int unitScale(IngredientUnit unit) {
		return unit == IngredientUnit.KG || unit == IngredientUnit.L ? 3 : 0;
	}
}
