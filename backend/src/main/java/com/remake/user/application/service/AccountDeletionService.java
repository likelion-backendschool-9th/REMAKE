package com.remake.user.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.remake.auth.application.port.in.DeleteRefreshTokensForUserUseCase;
import com.remake.auth.application.port.in.ExchangeOAuthProfileUseCase;
import com.remake.auth.domain.SocialProfile;
import com.remake.cart.application.port.in.DeleteCartItemsForUserUseCase;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.DeleteIngredientsForUserUseCase;
import com.remake.mealplan.application.port.in.DeleteMealPlansForUserUseCase;
import com.remake.notification.application.port.in.DeleteUserNotificationsUseCase;
import com.remake.quota.application.port.in.DeleteUsageLogsForUserUseCase;
import com.remake.receipt.application.port.in.DeleteReceiptsForUserUseCase;
import com.remake.recipe.application.port.in.DeleteSavedRecipesForUserUseCase;
import com.remake.user.application.port.in.DeleteAccountUseCase;
import com.remake.subscription.application.port.in.DeleteSubscriptionPaymentsForUserUseCase;
import com.remake.subscription.application.port.in.EndSubscriptionForWithdrawalUseCase;
import com.remake.user.application.port.in.PurgeExpiredDeletionCodesUseCase;
import com.remake.user.application.port.in.RequestAccountDeletionCodeUseCase;
import com.remake.user.application.port.out.LoadAccountDeletionRequestPort;
import com.remake.user.application.port.out.LoadUserPort;
import com.remake.user.application.port.out.SaveAccountDeletionRequestPort;
import com.remake.user.application.port.out.SaveExcludedIngredientPort;
import com.remake.user.application.port.out.SaveUserPort;
import com.remake.user.application.port.out.SendDeletionCodeMailPort;
import com.remake.user.domain.AccountDeletionRequest;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.DeletionMethod;
import com.remake.user.domain.User;

/**
 * 계정 삭제(API 9·10)와 하루 지난 인증번호 정리.
 * 미종료 구독은 탈퇴 직전에 즉시 종료하고(#258), 결제 기록(subscription_payments)은 파기한다(#350).
 */
@Service
public class AccountDeletionService
		implements RequestAccountDeletionCodeUseCase, DeleteAccountUseCase, PurgeExpiredDeletionCodesUseCase {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;
	private final LoadAccountDeletionRequestPort loadAccountDeletionRequestPort;
	private final SaveAccountDeletionRequestPort saveAccountDeletionRequestPort;
	private final SendDeletionCodeMailPort sendDeletionCodeMailPort;
	private final SaveExcludedIngredientPort saveExcludedIngredientPort;
	private final DeleteUserNotificationsUseCase deleteUserNotificationsUseCase;
	private final DeleteCartItemsForUserUseCase deleteCartItemsForUserUseCase;
	private final DeleteMealPlansForUserUseCase deleteMealPlansForUserUseCase;
	private final DeleteSavedRecipesForUserUseCase deleteSavedRecipesForUserUseCase;
	private final DeleteIngredientsForUserUseCase deleteIngredientsForUserUseCase;
	private final DeleteReceiptsForUserUseCase deleteReceiptsForUserUseCase;
	private final DeleteUsageLogsForUserUseCase deleteUsageLogsForUserUseCase;
	private final DeleteRefreshTokensForUserUseCase deleteRefreshTokensForUserUseCase;
	private final ExchangeOAuthProfileUseCase exchangeOAuthProfileUseCase;
	private final EndSubscriptionForWithdrawalUseCase endSubscriptionForWithdrawalUseCase;
	private final DeleteSubscriptionPaymentsForUserUseCase deleteSubscriptionPaymentsForUserUseCase;
	private final EventTracker eventTracker;
	private final Clock clock;
	private final TransactionTemplate requiresNewTransaction;

	public AccountDeletionService(LoadUserPort loadUserPort, SaveUserPort saveUserPort,
			LoadAccountDeletionRequestPort loadAccountDeletionRequestPort,
			SaveAccountDeletionRequestPort saveAccountDeletionRequestPort,
			SendDeletionCodeMailPort sendDeletionCodeMailPort, SaveExcludedIngredientPort saveExcludedIngredientPort,
			DeleteUserNotificationsUseCase deleteUserNotificationsUseCase,
			DeleteCartItemsForUserUseCase deleteCartItemsForUserUseCase,
			DeleteMealPlansForUserUseCase deleteMealPlansForUserUseCase,
			DeleteSavedRecipesForUserUseCase deleteSavedRecipesForUserUseCase,
			DeleteIngredientsForUserUseCase deleteIngredientsForUserUseCase,
			DeleteReceiptsForUserUseCase deleteReceiptsForUserUseCase,
			DeleteUsageLogsForUserUseCase deleteUsageLogsForUserUseCase,
			DeleteRefreshTokensForUserUseCase deleteRefreshTokensForUserUseCase,
			ExchangeOAuthProfileUseCase exchangeOAuthProfileUseCase,
			EndSubscriptionForWithdrawalUseCase endSubscriptionForWithdrawalUseCase,
			DeleteSubscriptionPaymentsForUserUseCase deleteSubscriptionPaymentsForUserUseCase,
			EventTracker eventTracker, Clock clock, PlatformTransactionManager transactionManager) {
		this.loadUserPort = loadUserPort;
		this.saveUserPort = saveUserPort;
		this.loadAccountDeletionRequestPort = loadAccountDeletionRequestPort;
		this.saveAccountDeletionRequestPort = saveAccountDeletionRequestPort;
		this.sendDeletionCodeMailPort = sendDeletionCodeMailPort;
		this.saveExcludedIngredientPort = saveExcludedIngredientPort;
		this.deleteUserNotificationsUseCase = deleteUserNotificationsUseCase;
		this.deleteCartItemsForUserUseCase = deleteCartItemsForUserUseCase;
		this.deleteMealPlansForUserUseCase = deleteMealPlansForUserUseCase;
		this.deleteSavedRecipesForUserUseCase = deleteSavedRecipesForUserUseCase;
		this.deleteIngredientsForUserUseCase = deleteIngredientsForUserUseCase;
		this.deleteReceiptsForUserUseCase = deleteReceiptsForUserUseCase;
		this.deleteUsageLogsForUserUseCase = deleteUsageLogsForUserUseCase;
		this.deleteRefreshTokensForUserUseCase = deleteRefreshTokensForUserUseCase;
		this.exchangeOAuthProfileUseCase = exchangeOAuthProfileUseCase;
		this.endSubscriptionForWithdrawalUseCase = endSubscriptionForWithdrawalUseCase;
		this.deleteSubscriptionPaymentsForUserUseCase = deleteSubscriptionPaymentsForUserUseCase;
		this.eventTracker = eventTracker;
		this.clock = clock;
		this.requiresNewTransaction = new TransactionTemplate(transactionManager);
		this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Override
	@Transactional
	public LocalDateTime requestCode(Long userId) {
		User user = requireActiveUser(userId);
		if (user.provider() == AuthProvider.NAVER) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "네이버 계정은 재인증으로 삭제합니다.");
		}
		if (!StringUtils.hasText(user.email())) {
			throw new BusinessException(ErrorCode.ACCOUNT_EMAIL_MISSING);
		}
		LocalDateTime now = LocalDateTime.now(clock);
		String plainCode = generateCode();
		AccountDeletionRequest request = AccountDeletionRequest.issue(userId, hash(plainCode),
				now.plusMinutes(AccountDeletionRequest.VALID_MINUTES), now);
		AccountDeletionRequest saved = saveAccountDeletionRequestPort.save(request);
		sendDeletionCodeMailPort.send(user.email(), plainCode);
		return saved.expiresAt();
	}

	@Override
	@Transactional
	public void delete(DeleteAccountUseCase.Command command) {
		User user = requireActiveUser(command.userId());
		DeletionMethod method = command.method();
		if (method == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "삭제 확인 방식을 보내 주세요.");
		}
		trackRequested(user, method);

		if (method == DeletionMethod.EMAIL_CODE) {
			verifyEmailCode(user, command.code());
		} else {
			verifyNaverReauth(user, command.oauthCode(), command.redirectUri(), command.state());
		}

		LocalDateTime now = LocalDateTime.now(clock);
		long tenureDays = tenureDays(user, now);

		endSubscriptionForWithdrawalUseCase.endForWithdrawal(user.id());
		deleteSubscriptionPaymentsForUserUseCase.deleteSubscriptionPaymentsForUser(user.id());
		deleteUserNotificationsUseCase.deleteUserNotifications(user.id());
		deleteCartItemsForUserUseCase.deleteCartItemsForUser(user.id());
		deleteMealPlansForUserUseCase.deleteMealPlansForUser(user.id());
		deleteSavedRecipesForUserUseCase.deleteSavedRecipesForUser(user.id());
		deleteIngredientsForUserUseCase.deleteIngredientsForUser(user.id());
		deleteReceiptsForUserUseCase.deleteReceiptsForUser(user.id());
		deleteUsageLogsForUserUseCase.deleteUsageLogsForUser(user.id());
		saveExcludedIngredientPort.replaceAll(user.id(), List.of());
		deleteRefreshTokensForUserUseCase.deleteAllForUser(user.id());
		saveAccountDeletionRequestPort.deleteAllByUserId(user.id());
		// 구독 종료가 plan을 BASIC으로 바꿨을 수 있어 다시 읽는다.
		User toDelete = requireActiveUser(user.id());
		saveUserPort.save(toDelete.withdraw(now));

		eventTracker.track(EventName.ACCOUNT_DELETED, Screen.ACCOUNT_DELETE_MODAL,
				Map.of("tenure_days", tenureDays, "deletion_method", method.eventValue()),
				EventTrackCommand.ofUser(user.id()));
	}

	@Override
	@Transactional
	public int purgeExpired() {
		LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(1);
		return saveAccountDeletionRequestPort.deleteCreatedBefore(cutoff);
	}

	private void verifyEmailCode(User user, String plainCode) {
		if (user.provider() == AuthProvider.NAVER) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "네이버 계정은 재인증으로 삭제합니다.");
		}
		if (!StringUtils.hasText(plainCode) || !plainCode.chars().allMatch(Character::isDigit)
				|| plainCode.length() != 6) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "인증번호 6자리를 입력해 주세요.");
		}
		AccountDeletionRequest request = loadAccountDeletionRequestPort.findLatestOpenByUserId(user.id())
				.orElseThrow(() -> new BusinessException(ErrorCode.DELETION_CODE_INVALID,
						ErrorCode.DELETION_CODE_INVALID.getMessage(),
						Map.of("remainingAttempts", AccountDeletionRequest.MAX_WRONG_ATTEMPTS)));

		LocalDateTime now = LocalDateTime.now(clock);
		if (request.isLocked()) {
			throw new BusinessException(ErrorCode.DELETION_CODE_LOCKED);
		}
		if (request.isExpired(now)) {
			throw new BusinessException(ErrorCode.DELETION_CODE_EXPIRED);
		}
		if (!request.codeHash().equals(hash(plainCode))) {
			// 바깥 TX 롤백과 무관하게, 동시에 틀려도 attempts가 겹치지 않게 DB에서 +1 한다.
			OptionalInt updatedAttempts = requiresNewTransaction.execute(status ->
					saveAccountDeletionRequestPort.incrementWrongAttempts(request.id()));
			if (updatedAttempts == null || updatedAttempts.isEmpty()) {
				throw new BusinessException(ErrorCode.DELETION_CODE_LOCKED);
			}
			int attempts = updatedAttempts.getAsInt();
			if (attempts >= AccountDeletionRequest.MAX_WRONG_ATTEMPTS) {
				throw new BusinessException(ErrorCode.DELETION_CODE_LOCKED);
			}
			throw new BusinessException(ErrorCode.DELETION_CODE_INVALID,
					ErrorCode.DELETION_CODE_INVALID.getMessage(),
					Map.of("remainingAttempts", AccountDeletionRequest.MAX_WRONG_ATTEMPTS - attempts));
		}
		request.markVerified(now);
		saveAccountDeletionRequestPort.save(request);
	}

	private void verifyNaverReauth(User user, String oauthCode, String redirectUri, String state) {
		if (user.provider() != AuthProvider.NAVER) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "이메일 인증번호로 삭제해 주세요.");
		}
		if (!StringUtils.hasText(oauthCode) || !StringUtils.hasText(redirectUri)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "네이버 재인증 값이 필요합니다.");
		}
		SocialProfile profile = exchangeOAuthProfileUseCase.exchange(AuthProvider.NAVER, oauthCode, redirectUri, state);
		if (!user.providerUserId().equals(profile.providerUserId())) {
			throw new BusinessException(ErrorCode.REAUTH_ACCOUNT_MISMATCH);
		}
	}

	private void trackRequested(User user, DeletionMethod method) {
		eventTracker.track(EventName.ACCOUNT_DELETION_REQUESTED, Screen.ACCOUNT_DELETE_CODE,
				Map.of("provider", user.provider().wireName(), "deletion_method", method.eventValue()),
				EventTrackCommand.ofUser(user.id()));
	}

	private User requireActiveUser(Long userId) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		return user;
	}

	private static long tenureDays(User user, LocalDateTime now) {
		LocalDateTime createdAt = user.createdAt() != null ? user.createdAt() : now;
		return Math.max(0, ChronoUnit.DAYS.between(createdAt.toLocalDate(), now.toLocalDate()));
	}

	private static String generateCode() {
		int value = RANDOM.nextInt(1_000_000);
		return String.format(Locale.ROOT, "%06d", value);
	}

	private static String hash(String plainCode) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(plainCode.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
