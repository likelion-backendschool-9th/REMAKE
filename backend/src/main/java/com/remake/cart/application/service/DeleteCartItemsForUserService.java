package com.remake.cart.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.DeleteCartItemsForUserUseCase;
import com.remake.cart.application.port.out.DeleteCartItemsForUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class DeleteCartItemsForUserService implements DeleteCartItemsForUserUseCase {

	private final DeleteCartItemsForUserPort deleteCartItemsForUserPort;

	@Override
	@Transactional
	public void deleteCartItemsForUser(Long userId) {
		deleteCartItemsForUserPort.deleteAllByUserId(userId);
	}
}
