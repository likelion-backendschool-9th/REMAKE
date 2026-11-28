package com.remake.subscription.application.port.out;

import java.util.Optional;

import com.remake.subscription.domain.Subscription;

public interface LoadSubscriptionPort {

	Optional<Subscription> findUnfinishedByUserId(Long userId);

	Optional<Subscription> findByCustomerKey(String customerKey);
}
