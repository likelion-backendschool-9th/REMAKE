package com.remake.user.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.user.domain.SubscriptionPayment;
import com.remake.user.domain.SubscriptionPaymentStatus;

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
@Table(name = "subscription_payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SubscriptionPaymentJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subscription_id", nullable = false)
	private Long subscriptionId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "order_id", nullable = false, length = 64)
	private String orderId;

	@Column(name = "payment_key", length = 200)
	private String paymentKey;

	@Column(nullable = false)
	private long amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SubscriptionPaymentStatus status;

	@Column(name = "failure_code", length = 64)
	private String failureCode;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	@Column(name = "period_end", nullable = false)
	private LocalDate periodEnd;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	static SubscriptionPaymentJpaEntity from(SubscriptionPayment payment) {
		SubscriptionPaymentJpaEntity entity = new SubscriptionPaymentJpaEntity();
		entity.id = payment.id();
		entity.apply(payment);
		return entity;
	}

	void apply(SubscriptionPayment payment) {
		this.subscriptionId = payment.subscriptionId();
		this.userId = payment.userId();
		this.orderId = payment.orderId();
		this.paymentKey = payment.paymentKey();
		this.amount = payment.amount();
		this.status = payment.status();
		this.failureCode = payment.failureCode();
		this.periodStart = payment.periodStart();
		this.periodEnd = payment.periodEnd();
		this.approvedAt = payment.approvedAt();
	}

	SubscriptionPayment toDomain() {
		return SubscriptionPayment.restore(id, subscriptionId, userId, orderId, paymentKey, amount, status, failureCode,
				periodStart, periodEnd, approvedAt, getCreatedAt());
	}
}
