package com.remake.receipt.application.service;

import java.math.BigDecimal;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Item;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.ItemRequest;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Volume;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.VolumeScope;

final class ReceiptQuantity {

	private ReceiptQuantity() {
	}

	static Item resolve(ItemRequest request, IngredientUnit unit) {
		BigDecimal amount = request.amount();
		BigDecimal purchaseQuantity = request.purchaseQuantity();
		Volume volume = request.volume();

		if (volume != null && !isMeasurement(volume.unit())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "용량에는 무게 또는 부피 단위를 사용해 주세요.");
		}
		if (amount == null) {
			if (!isMeasurement(unit)) {
				amount = purchaseQuantity;
			}
			else if (volume != null && (volume.scope() == VolumeScope.TOTAL || purchaseQuantity != null)) {
				amount = convert(volume, unit);
				if (volume.scope() == VolumeScope.PER_PACK) {
					amount = amount.multiply(purchaseQuantity);
				}
			}
			else if (purchaseQuantity != null) {
				amount = purchaseQuantity;
				unit = IngredientUnit.PACK;
			}
		}

		if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 3
				|| amount.compareTo(new BigDecimal("10000000")) >= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "등록할 품목 수량을 확인해 주세요.");
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

	private static boolean isMeasurement(IngredientUnit unit) {
		return switch (unit) {
			case G, KG, ML, L -> true;
			default -> false;
		};
	}

	private static BigDecimal convert(Volume volume, IngredientUnit target) {
		IngredientUnit source = volume.unit();
		BigDecimal amount = volume.amount();
		if (source == target) {
			return amount;
		}
		return switch (source) {
			case G -> target == IngredientUnit.KG ? amount.movePointLeft(3) : incompatibleVolume();
			case KG -> target == IngredientUnit.G ? amount.movePointRight(3) : incompatibleVolume();
			case ML -> target == IngredientUnit.L ? amount.movePointLeft(3) : incompatibleVolume();
			case L -> target == IngredientUnit.ML ? amount.movePointRight(3) : incompatibleVolume();
			default -> incompatibleVolume();
		};
	}

	private static BigDecimal incompatibleVolume() {
		throw new BusinessException(ErrorCode.INVALID_INPUT, "품목 수량과 용량의 단위 종류가 다릅니다.");
	}
}
