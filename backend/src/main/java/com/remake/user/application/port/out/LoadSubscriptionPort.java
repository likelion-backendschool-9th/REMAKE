package com.remake.user.application.port.out;

import java.util.Optional;

import com.remake.user.domain.Subscription;

public interface LoadSubscriptionPort {

	Optional<Subscription> findUnfinishedByUserId(Long userId);

	Optional<Subscription> findByCustomerKey(String customerKey);
}
