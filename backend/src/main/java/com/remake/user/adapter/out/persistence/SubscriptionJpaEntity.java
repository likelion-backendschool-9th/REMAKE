package com.remake.user.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.remake.global.common.BaseTimeEntity;
import com.remake.user.domain.Subscription;
import com.remake.user.domain.SubscriptionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SubscriptionJpaEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriptionStatus status;

	@Column(name = "customer_key", nullable = false, length = 100)
	private String customerKey;

	@Column(name = "billing_key", length = 512)
	private String billingKey;

	@Column(nullable = false)
	private long price;

	@Column(name = "current_period_start", nullable = false)
	private LocalDate currentPeriodStart;

	@Column(name = "next_billing_date", nullable = false)
	private LocalDate nextBillingDate;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	static SubscriptionJpaEntity from(Subscription subscription) {
		SubscriptionJpaEntity entity = new SubscriptionJpaEntity();
		entity.id = subscription.id();
		entity.apply(subscription);
		return entity;
	}

	void apply(Subscription subscription) {
		this.userId = subscription.userId();
		this.status = subscription.status();
		this.customerKey = subscription.customerKey();
		this.billingKey = subscription.billingKeyEncrypted();
		this.price = subscription.price();
		this.currentPeriodStart = subscription.currentPeriodStart();
		this.nextBillingDate = subscription.nextBillingDate();
		this.canceledAt = subscription.canceledAt();
		this.endedAt = subscription.endedAt();
	}

	Subscription toDomain() {
		return Subscription.restore(id, userId, status, customerKey, billingKey, price, currentPeriodStart,
				nextBillingDate, canceledAt, endedAt, getCreatedAt());
	}
}
