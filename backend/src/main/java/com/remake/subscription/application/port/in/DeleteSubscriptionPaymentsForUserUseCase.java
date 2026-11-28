package com.remake.subscription.application.port.in;

/** 회원 탈퇴 때 부른다. 그 회원의 결제 기록(subscription_payments)을 모두 지운다(#350). */
public interface DeleteSubscriptionPaymentsForUserUseCase {

	void deleteSubscriptionPaymentsForUser(Long userId);
}
