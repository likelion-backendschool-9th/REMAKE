package com.remake.subscription.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.subscription.application.port.in.CancelSubscriptionUseCase;
import com.remake.subscription.application.port.in.EndSubscriptionForWithdrawalUseCase;
import com.remake.subscription.application.port.in.GetSubscriptionUseCase;
import com.remake.subscription.application.port.in.PrepareBillingAuthUseCase;
import com.remake.subscription.application.port.in.StartSubscriptionUseCase;
import com.remake.subscription.application.port.out.BillingKeyCipherPort;
import com.remake.subscription.application.port.out.LoadSubscriptionPort;
import com.remake.subscription.application.port.out.SaveSubscriptionPaymentPort;
import com.remake.subscription.application.port.out.SaveSubscriptionPort;
import com.remake.subscription.application.port.out.TossBillingPort;
import com.remake.subscription.domain.Subscription;
import com.remake.subscription.domain.SubscriptionPayment;
import com.remake.user.application.port.in.ChangeUserPlanUseCase;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.User;
import com.remake.user.domain.UserPlan;

/**
 * 구독 조회·카드 등록 준비·첫 결제·해지 예약(#167)과 탈퇴 시 즉시 종료(#258).
 * 정기 자동결제 작업은 #168. 회원 조회·plan 변경은 user UseCase만 쓴다(#344).
 */
@Service
public class SubscriptionService implements GetSubscriptionUseCase, PrepareBillingAuthUseCase, StartSubscriptionUseCase,
		CancelSubscriptionUseCase, EndSubscriptionForWithdrawalUseCase {

	private final GetUserUseCase getUserUseCase;
	private final ChangeUserPlanUseCase changeUserPlanUseCase;
	private final LoadSubscriptionPort loadSubscriptionPort;
	private final SaveSubscriptionPort saveSubscriptionPort;
	private final SaveSubscriptionPaymentPort saveSubscriptionPaymentPort;
	private final TossBillingPort tossBillingPort;
	private final BillingKeyCipherPort billingKeyCipherPort;
	private final EventTracker eventTracker;
	private final Clock clock;
	private final TransactionTemplate transactionTemplate;

	public SubscriptionService(GetUserUseCase getUserUseCase, ChangeUserPlanUseCase changeUserPlanUseCase,
			LoadSubscriptionPort loadSubscriptionPort, SaveSubscriptionPort saveSubscriptionPort,
			SaveSubscriptionPaymentPort saveSubscriptionPaymentPort, TossBillingPort tossBillingPort,
			BillingKeyCipherPort billingKeyCipherPort, EventTracker eventTracker, Clock clock,
			PlatformTransactionManager transactionManager) {
		this.getUserUseCase = getUserUseCase;
		this.changeUserPlanUseCase = changeUserPlanUseCase;
		this.loadSubscriptionPort = loadSubscriptionPort;
		this.saveSubscriptionPort = saveSubscriptionPort;
		this.saveSubscriptionPaymentPort = saveSubscriptionPaymentPort;
		this.tossBillingPort = tossBillingPort;
		this.billingKeyCipherPort = billingKeyCipherPort;
		this.eventTracker = eventTracker;
		this.clock = clock;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	@Transactional(readOnly = true)
	public SubscriptionView get(Long userId) {
		requireActiveUser(userId);
		return loadSubscriptionPort.findUnfinishedByUserId(userId)
				.map(this::toView)
				.orElseGet(this::noneView);
	}

	@Override
	@Transactional(readOnly = true)
	public BillingAuthView prepare(Long userId) {
		requireActiveUser(userId);
		rejectIfAlreadySubscribed(userId);
		return new BillingAuthView(UUID.randomUUID().toString(), Subscription.ORDER_NAME,
				Subscription.MONTHLY_PRICE_KRW);
	}

	@Override
	public SubscriptionView start(Command command) {
		User user = requireActiveUser(command.userId());
		rejectIfAlreadySubscribed(user.id());
		if (!StringUtils.hasText(command.authKey()) || !StringUtils.hasText(command.customerKey())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "카드 등록 결과를 확인해 주세요.");
		}
		loadSubscriptionPort.findByCustomerKey(command.customerKey()).ifPresent(existing -> {
			if (!existing.userId().equals(user.id())) {
				throw new BusinessException(ErrorCode.BILLING_AUTH_FAILED);
			}
			if (existing.isUnfinished()) {
				throw new BusinessException(ErrorCode.ALREADY_SUBSCRIBED);
			}
		});

		String plainBillingKey;
		try {
			plainBillingKey = tossBillingPort.issueBillingKey(command.authKey(), command.customerKey()).billingKey();
		} catch (BusinessException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new BusinessException(ErrorCode.BILLING_AUTH_FAILED);
		}
		if (!StringUtils.hasText(plainBillingKey)) {
			throw new BusinessException(ErrorCode.BILLING_AUTH_FAILED);
		}

		// 결제 전에 암호화한다. TOSS_BILLING_KEY_SECRET이 비면 charge 전에 실패해 과금만 되는 상태를 막는다.
		String encryptedKey = billingKeyCipherPort.encrypt(plainBillingKey);

		LocalDateTime now = LocalDateTime.now(clock);
		LocalDate today = now.toLocalDate();
		String orderId = "sub_" + UUID.randomUUID().toString().replace("-", "");
		TossBillingPort.ChargedPayment charged;
		try {
			// 결제 실패 이벤트는 롤백되는 @Transactional 밖에서 남겨 afterCommit이 실행되게 한다.
			charged = tossBillingPort.charge(plainBillingKey, command.customerKey(), orderId, Subscription.ORDER_NAME,
					Subscription.MONTHLY_PRICE_KRW);
		} catch (BusinessException ex) {
			trackPaymentFailed(user.id(), ex.getErrorCode().name());
			throw ex;
		} catch (RuntimeException ex) {
			trackPaymentFailed(user.id(), ErrorCode.PAYMENT_UPSTREAM_ERROR.name());
			throw new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR);
		}

		// 첫 결제 실패 시 구독 행을 만들지 않는다. subscription_payments.subscription_id가 NOT NULL이라
		// FAILED 결제 행은 남기지 않고 E85만 기록한다(errors.md의 FAILED 행 문구와 ERD 충돌).
		String customerKey = command.customerKey();
		String paymentKey = charged.paymentKey();
		return transactionTemplate.execute(status -> {
			Subscription saved = saveSubscriptionPort.save(
					Subscription.start(user.id(), customerKey, encryptedKey, today, now));
			LocalDate periodEnd = saved.nextBillingDate();
			saveSubscriptionPaymentPort.save(SubscriptionPayment.done(saved.id(), user.id(), orderId, paymentKey,
					Subscription.MONTHLY_PRICE_KRW, today, periodEnd, now));
			changeUserPlanUseCase.changePlan(user.id(), UserPlan.PREMIUM);
			eventTracker.track(EventName.SUBSCRIPTION_STARTED, Screen.SUBSCRIPTION_MANAGE,
					Map.of("payment_amount", Subscription.MONTHLY_PRICE_KRW),
					EventTrackCommand.ofUser(user.id()));
			return toView(saved);
		});
	}

	@Override
	@Transactional
	public SubscriptionView cancel(Long userId) {
		requireActiveUser(userId);
		Subscription subscription = loadSubscriptionPort.findUnfinishedByUserId(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
		if (subscription.status() == com.remake.subscription.domain.SubscriptionStatus.CANCELED) {
			return toView(subscription);
		}
		LocalDateTime now = LocalDateTime.now(clock);
		Subscription canceled = saveSubscriptionPort.save(subscription.scheduleCancel(now));
		eventTracker.track(EventName.SUBSCRIPTION_CANCELED, Screen.SUBSCRIPTION_MANAGE,
				Map.of("subscribed_days", canceled.subscribedDays(now)),
				EventTrackCommand.ofUser(userId));
		return toView(canceled);
	}

	@Override
	@Transactional
	public void endForWithdrawal(Long userId) {
		LocalDateTime now = LocalDateTime.now(clock);
		loadSubscriptionPort.findUnfinishedByUserId(userId).ifPresent(subscription -> {
			int updated = saveSubscriptionPort.endUnfinished(subscription.id(), now);
			if (updated == 0) {
				return;
			}
			changeUserPlanUseCase.changePlan(userId, UserPlan.BASIC);
			eventTracker.track(EventName.SUBSCRIPTION_ENDED, Screen.SETTING_MAIN,
					Map.of("end_reason", "account_deleted", "subscribed_days", subscription.subscribedDays(now)),
					EventTrackCommand.ofUser(userId));
		});
	}

	private void trackPaymentFailed(Long userId, String errorCode) {
		eventTracker.track(EventName.SUBSCRIPTION_PAYMENT_FAILED, Screen.SUBSCRIPTION_MANAGE,
				Map.of("is_renewal", false, "error_code", errorCode),
				EventTrackCommand.ofUser(userId));
	}

	private void rejectIfAlreadySubscribed(Long userId) {
		if (loadSubscriptionPort.findUnfinishedByUserId(userId).isPresent()) {
			throw new BusinessException(ErrorCode.ALREADY_SUBSCRIBED);
		}
	}

	private User requireActiveUser(Long userId) {
		User user = getUserUseCase.getUser(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return user;
	}

	private SubscriptionView noneView() {
		LocalDate next = Subscription.nextBillingDate(LocalDate.now(clock));
		return new SubscriptionView(SubscriptionViewStatus.NONE, Subscription.MONTHLY_PRICE_KRW, next, null);
	}

	private SubscriptionView toView(Subscription subscription) {
		SubscriptionViewStatus status = SubscriptionViewStatus.from(subscription.status());
		LocalDate periodEnd = status == SubscriptionViewStatus.CANCELED
				? subscription.nextBillingDate().minusDays(1)
				: null;
		return new SubscriptionView(status, subscription.price(), subscription.nextBillingDate(), periodEnd);
	}
}
