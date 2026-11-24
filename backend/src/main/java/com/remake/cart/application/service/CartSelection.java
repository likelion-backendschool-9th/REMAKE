package com.remake.cart.application.service;

import java.util.HashSet;
import java.util.List;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

/** 여러 항목 변경의 입력 검사. 잠금은 항상 id 오름차순으로 획득한다. */
final class CartSelection {
	private CartSelection() { }

	static List<Long> validate(Long userId, List<Long> ids) {
		if (userId == null || userId <= 0 || ids == null || ids.isEmpty()
				|| ids.stream().anyMatch(id -> id == null || id <= 0)
				|| new HashSet<>(ids).size() != ids.size()) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		return ids.stream().sorted().toList();
	}
}
