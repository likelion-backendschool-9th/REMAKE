package com.remake.cart.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.AutomaticCartDeductionCommand;
import com.remake.cart.application.port.in.CartDeductionCommand;
import com.remake.cart.application.port.in.DeductCartItemsUseCase;
import com.remake.cart.application.port.out.LoadCartItemPort;
import com.remake.cart.application.port.out.SaveCartItemPort;
import com.remake.cart.domain.CartItem;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartDeductionService implements DeductCartItemsUseCase {

	private final LoadCartItemPort loadCartItemPort;
	private final SaveCartItemPort saveCartItemPort;

	private record MatchKey(String name, String unit) {}

	@Override
	@Transactional
	public int deductAutomatically(Long userId, List<AutomaticCartDeductionCommand> deductions) {
		if (userId == null || userId <= 0 || deductions == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		Map<MatchKey, BigDecimal> remaining = new HashMap<>();
		for (var command : deductions) {
			if (command == null || command.ingredientName() == null || command.ingredientName().isBlank()
					|| command.unit() == null) {
				throw new BusinessException(ErrorCode.INVALID_INPUT);
			}
			try {
				CartItem.validateAmount(command.amount());
			} catch (IllegalArgumentException e) {
				throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
			}
			remaining.merge(new MatchKey(command.ingredientName(), command.unit().name()), command.amount(),
					BigDecimal::add);
		}
		if (remaining.isEmpty()) {
			return 0;
		}
		int removedCount = 0;
		// 입력 순서와 무관하게 모든 행을 ID 순서로 잠가 교착과 동시 초과 차감을 막는다.
		for (CartItem item : loadCartItemPort.loadOwnedForUpdate(userId)) {
			var key = new MatchKey(item.name(), item.unit().name());
			BigDecimal requested = remaining.get(key);
			if (requested == null || requested.signum() == 0) {
				continue;
			}
			BigDecimal amount = requested.min(item.amount());
			CartItem changed = item.subtract(amount);
			remaining.put(key, requested.subtract(amount));
			if (changed.amount().signum() == 0) {
				saveCartItemPort.delete(changed.id());
				removedCount++;
			} else {
				saveCartItemPort.save(changed);
			}
		}
		return removedCount;
	}

	@Override
	@Transactional
	public int deduct(Long userId, List<CartDeductionCommand> deductions) {
		if (userId == null || userId <= 0 || deductions == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		// 항상 id 오름차순으로 잠가, 여러 대상을 역순으로 보낸 동시 요청도 잠금 순서가 같게 한다.
		Map<Long, List<CartDeductionCommand>> byId = new TreeMap<>();
		for (CartDeductionCommand command : deductions) {
			validate(command);
			byId.computeIfAbsent(command.cartItemId(), ignored -> new ArrayList<>()).add(command);
		}

		// 모든 행을 검증한 뒤 변경한다. 부분 검증 성공만으로 앞 행을 차감하지 않는다.
		List<CartItem> changed = new ArrayList<>();
		for (var entry : byId.entrySet()) {
			CartItem item = loadCartItemPort.loadOwnedForUpdate(userId, entry.getKey())
					.orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
			BigDecimal total = BigDecimal.ZERO;
			for (CartDeductionCommand command : entry.getValue()) {
				if (!item.name().equals(command.ingredientName())
						|| !item.unit().name().equals(command.unit().name())) {
					throw new BusinessException(ErrorCode.INVALID_INPUT);
				}
				total = total.add(command.amount());
			}
			if (total.compareTo(item.amount()) > 0) {
				throw new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY,
						ErrorCode.INSUFFICIENT_QUANTITY.getMessage(), Map.of("remaining", item.amount()));
			}
			changed.add(item.subtract(total));
		}
		int removedCount = 0;
		for (CartItem item : changed) {
			if (item.amount().signum() == 0) {
				saveCartItemPort.delete(item.id());
				removedCount++;
			} else {
				saveCartItemPort.save(item);
			}
		}
		return removedCount;
	}

	private void validate(CartDeductionCommand command) {
		if (command == null || command.cartItemId() == null || command.cartItemId() <= 0
				|| command.ingredientName() == null || command.ingredientName().isBlank()
				|| command.unit() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		try {
			CartItem.validateAmount(command.amount());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
	}
}
