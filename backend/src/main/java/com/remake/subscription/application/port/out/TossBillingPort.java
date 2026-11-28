package com.remake.subscription.application.port.out;

/**
 * 토스 자동결제: billingKey 발급·청구·결제 취소.
 * 시크릿·billingKey·paymentKey는 호출부·로그에 남기지 않는다.
 */
public interface TossBillingPort {

	IssuedBillingKey issueBillingKey(String authKey, String customerKey);

	ChargedPayment charge(String billingKey, String customerKey, String orderId, String orderName, long amount);

	/** 결제 전액(또는 일부) 취소. 환불 요청(#343)에서 쓴다. */
	void cancelPayment(String paymentKey, String cancelReason, long cancelAmount);

	record IssuedBillingKey(String billingKey) {
	}

	record ChargedPayment(String paymentKey, long amount) {
	}
}
