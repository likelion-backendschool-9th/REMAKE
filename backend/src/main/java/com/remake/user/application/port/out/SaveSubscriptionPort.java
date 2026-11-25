package com.remake.user.application.port.out;

import java.time.LocalDateTime;

import com.remake.user.domain.Subscription;

public interface SaveSubscriptionPort {

	Subscription save(Subscription subscription);

	/** 미종료 구독을 ENDED로 바꾸고 billingKey를 비운다. 변경된 행 수. */
	int endUnfinished(Long subscriptionId, LocalDateTime endedAt);
}
