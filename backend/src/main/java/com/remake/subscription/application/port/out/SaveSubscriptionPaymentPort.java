package com.remake.subscription.application.port.out;

import com.remake.subscription.domain.SubscriptionPayment;

public interface SaveSubscriptionPaymentPort {

	SubscriptionPayment save(SubscriptionPayment payment);
}
