package com.remake.user.application.port.out;

/**
 * 토스 자동결제: billingKey 발급·청구.
 * 시크릿·billingKey는 호출부·로그에 남기지 않는다.
 */
public interface TossBillingPort {

	IssuedBillingKey issueBillingKey(String authKey, String customerKey);

	ChargedPayment charge(String billingKey, String customerKey, String orderId, String orderName, long amount);

	record IssuedBillingKey(String billingKey) {
	}

	record ChargedPayment(String paymentKey, long amount) {
	}
}
