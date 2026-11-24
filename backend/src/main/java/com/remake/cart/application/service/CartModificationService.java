package com.remake.cart.application.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.DeleteCartItemsUseCase;
import com.remake.cart.application.port.in.UpdateCartItemUseCase;
import com.remake.cart.application.port.out.LoadCartItemPort;
import com.remake.cart.application.port.out.SaveCartItemPort;
import com.remake.cart.domain.CartItem;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CartModificationService implements UpdateCartItemUseCase, DeleteCartItemsUseCase {
	private final LoadCartItemPort cart;
	private final SaveCartItemPort changes;

	@Override
	@Transactional
	public CartItem updateAmount(Long userId, Long cartItemId, BigDecimal amount) {
		CartSelection.validate(userId, java.util.Collections.singletonList(cartItemId));
		try {
			CartItem.validateAmount(amount);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
		CartItem item = cart.loadOwnedForUpdate(userId, cartItemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
		CartItem changed = new CartItem(item.id(), item.userId(), item.recipeId(), item.mealPlanSlotId(),
				item.categoryId(), item.name(), amount, item.unit());
		changes.save(changed);
		return changed;
	}

	@Override
	@Transactional
	public void deleteItems(Long userId, List<Long> ids) {
		List<Long> sorted = CartSelection.validate(userId, ids);
		List<CartItem> items = cart.loadOwnedForUpdate(userId, sorted);
		if (items.size() != sorted.size()) {
			throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
		}
		items.forEach(item -> changes.delete(item.id()));
	}
}
