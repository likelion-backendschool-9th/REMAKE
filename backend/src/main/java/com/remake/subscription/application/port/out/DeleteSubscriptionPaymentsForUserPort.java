package com.remake.subscription.application.port.out;

public interface DeleteSubscriptionPaymentsForUserPort {

	void deleteAllByUserId(Long userId);
}
