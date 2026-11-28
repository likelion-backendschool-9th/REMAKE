package com.remake.subscription.application.port.out;

import java.util.Optional;

import com.remake.subscription.domain.SubscriptionPayment;

/** 구독 결제 행 조회. */
public interface LoadSubscriptionPaymentPort {

	/** 해당 구독의 가장 최근 DONE 결제. 환불 취소 대상(#343). */
	Optional<SubscriptionPayment> findLatestDoneBySubscriptionId(Long subscriptionId);
}
