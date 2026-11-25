package com.remake.user.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.remake.user.application.port.out.LoadSubscriptionPort;
import com.remake.user.application.port.out.SaveSubscriptionPaymentPort;
import com.remake.user.application.port.out.SaveSubscriptionPort;
import com.remake.user.domain.Subscription;
import com.remake.user.domain.SubscriptionPayment;
import com.remake.user.domain.SubscriptionStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SubscriptionPersistenceAdapter
		implements LoadSubscriptionPort, SaveSubscriptionPort, SaveSubscriptionPaymentPort {

	private static final EnumSet<SubscriptionStatus> UNFINISHED =
			EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED);

	private final SubscriptionJpaRepository subscriptionRepository;
	private final SubscriptionPaymentJpaRepository paymentRepository;

	@Override
	public Optional<Subscription> findUnfinishedByUserId(Long userId) {
		return subscriptionRepository.findByUserIdAndStatusIn(userId, UNFINISHED).map(SubscriptionJpaEntity::toDomain);
	}

	@Override
	public Optional<Subscription> findByCustomerKey(String customerKey) {
		return subscriptionRepository.findByCustomerKey(customerKey).map(SubscriptionJpaEntity::toDomain);
	}

	@Override
	public Subscription save(Subscription subscription) {
		SubscriptionJpaEntity entity = subscription.id() == null
				? SubscriptionJpaEntity.from(subscription)
				: subscriptionRepository.findById(subscription.id()).map(existing -> {
					existing.apply(subscription);
					return existing;
				}).orElseGet(() -> SubscriptionJpaEntity.from(subscription));
		return subscriptionRepository.save(entity).toDomain();
	}

	@Override
	@Transactional
	public int endUnfinished(Long subscriptionId, LocalDateTime endedAt) {
		return subscriptionRepository.endUnfinished(subscriptionId, endedAt, UNFINISHED);
	}

	@Override
	public SubscriptionPayment save(SubscriptionPayment payment) {
		SubscriptionPaymentJpaEntity entity = payment.id() == null
				? SubscriptionPaymentJpaEntity.from(payment)
				: paymentRepository.findById(payment.id()).map(existing -> {
					existing.apply(payment);
					return existing;
				}).orElseGet(() -> SubscriptionPaymentJpaEntity.from(payment));
		return paymentRepository.save(entity).toDomain();
	}
}
