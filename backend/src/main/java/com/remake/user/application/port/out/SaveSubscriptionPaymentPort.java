package com.remake.user.application.port.out;

import com.remake.user.domain.SubscriptionPayment;

public interface SaveSubscriptionPaymentPort {

	SubscriptionPayment save(SubscriptionPayment payment);
}
