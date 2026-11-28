package com.remake.user.application.port.out;

public interface DeleteSubscriptionPaymentsForUserPort {

	void deleteAllByUserId(Long userId);
}
