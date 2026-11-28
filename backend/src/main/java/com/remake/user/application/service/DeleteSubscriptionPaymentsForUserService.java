package com.remake.user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.user.application.port.in.DeleteSubscriptionPaymentsForUserUseCase;
import com.remake.user.application.port.out.DeleteSubscriptionPaymentsForUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class DeleteSubscriptionPaymentsForUserService implements DeleteSubscriptionPaymentsForUserUseCase {

	private final DeleteSubscriptionPaymentsForUserPort deleteSubscriptionPaymentsForUserPort;

	@Override
	@Transactional
	public void deleteSubscriptionPaymentsForUser(Long userId) {
		deleteSubscriptionPaymentsForUserPort.deleteAllByUserId(userId);
	}
}
