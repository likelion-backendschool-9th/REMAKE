package com.remake.recipe.application.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.quota.application.port.in.GetUsageQuotasUseCase;
import com.remake.quota.domain.RecipeQuota;
import com.remake.recipe.application.port.in.GenerateRecipeCommand;
import com.remake.recipe.application.port.in.GenerateRecipeUseCase;
import com.remake.recipe.application.port.out.GenerateRecipeAiPort;
import com.remake.recipe.application.port.out.GeneratedRecipe;
import com.remake.recipe.application.port.out.RecipeAiRequest;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.CustomOptions;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.User;

import lombok.extern.slf4j.Slf4j;

/**
 * 레시피 생성(API 28).
 * <ol>
 * <li>입력 확인과 재료 고르기(INVALID_INPUT, INGREDIENT_NOT_FOUND)</li>
 * <li>남은 횟수 확인(GENERATION_LIMIT_EXCEEDED, quota_exceeded 이벤트)</li>
 * <li>재료 보충 OFF면 재료가 충분한지(NO_INGREDIENTS)</li>
 * <li>AI 호출과 응답 검증. 실패하면 잠깐 쉬고 1회 다시 시도(AI_* 코드)</li>
 * <li>한 트랜잭션에서 레시피 저장 + 횟수 차감</li>
 * </ol>
 * AI 호출은 트랜잭션 밖에서 한다(최대 60초 동안 DB 연결을 잡지 않음). 실패는 차감하지 않고 이벤트로만 남긴다.
 */
@Slf4j
@Service
class RecipeGenerationService implements GenerateRecipeUseCase {

	private static final int MAX_AI_ATTEMPTS = 2;
	private static final Set<ErrorCode> RETRYABLE = Set.of(ErrorCode.AI_UPSTREAM_ERROR, ErrorCode.AI_TIMEOUT,
			ErrorCode.AI_INVALID_RESPONSE);
	/** 가구원 수를 아직 정하지 않은 회원의 빠른 생성 인분 */
	private static final int DEFAULT_SERVINGS = 1;
	private static final int MAX_SERVINGS = 50;

	private final GetUserUseCase getUserUseCase;
	private final GetFridgeUseCase getFridgeUseCase;
	private final ConsumeRecipeQuotaUseCase consumeRecipeQuotaUseCase;
	private final GetUsageQuotasUseCase getUsageQuotasUseCase;
	private final GenerateRecipeAiPort aiPort;
	private final SaveRecipePort saveRecipePort;
	private final EventTracker eventTracker;
	private final TransactionTemplate transactionTemplate;
	private final Duration retryDelay;

	RecipeGenerationService(GetUserUseCase getUserUseCase, GetFridgeUseCase getFridgeUseCase,
			ConsumeRecipeQuotaUseCase consumeRecipeQuotaUseCase, GetUsageQuotasUseCase getUsageQuotasUseCase,
			GenerateRecipeAiPort aiPort,
			SaveRecipePort saveRecipePort, EventTracker eventTracker, TransactionTemplate transactionTemplate,
			@Value("${remake.recipe.ai-retry-delay:1500ms}") Duration retryDelay) {
		this.getUserUseCase = getUserUseCase;
		this.getFridgeUseCase = getFridgeUseCase;
		this.consumeRecipeQuotaUseCase = consumeRecipeQuotaUseCase;
		this.getUsageQuotasUseCase = getUsageQuotasUseCase;
		this.aiPort = aiPort;
		this.saveRecipePort = saveRecipePort;
		this.eventTracker = eventTracker;
		this.transactionTemplate = transactionTemplate;
		this.retryDelay = retryDelay;
	}

	@Override
	public Recipe generate(Long userId, GenerateRecipeCommand command) {
		User user = getUserUseCase.getUser(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		RecipeSource mode = command.mode();
		if (mode != RecipeSource.QUICK && mode != RecipeSource.CUSTOM) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "생성 방식은 QUICK 또는 CUSTOM입니다.");
		}
		int servings = servingsOf(user, command);
		List<FridgeIngredient> selected = GenerationIngredients.select(
				getFridgeUseCase.getFridgeIngredients(userId, null, null, null),
				mode == RecipeSource.CUSTOM ? command.ingredientIds() : List.of());
		CustomOptions options = mode == RecipeSource.CUSTOM
				? new CustomOptions(servings, command.ingredientIds(), command.taste(), command.cookTime(),
						command.tool(), command.method(), command.cuisine(), command.allergenCodes())
				: null;
		// 빠른 생성의 제외 재료는 설정의 알레르기(user_excluded_ingredients)를 써야 하지만 아직 그 테이블이 없어 비워 둔다.
		List<String> allergens = mode == RecipeSource.CUSTOM ? command.allergenCodes() : List.of();
		RecipeAiRequest request = new RecipeAiRequest(mode, servings, user.recipeSupplementEnabled(),
				GenerationIngredients.toAiItems(selected), options, allergens);
		return run(userId, request, selected, false);
	}

	/** 횟수 확인부터 저장까지. 다시 받기(API 29)도 이 흐름을 쓴다. */
	Recipe run(Long userId, RecipeAiRequest request, List<FridgeIngredient> selected, boolean regenerated) {
		long start = System.nanoTime();
		// 이벤트 값은 EventPropertyValues 허용 목록대로 대문자(QUICK·CUSTOM, RECIPE)를 쓴다
		String mode = request.source().name();
		Screen screen = request.source() == RecipeSource.CUSTOM ? Screen.RECIPE_CUSTOM : Screen.RECIPE_CREATE;

		requireQuota(userId, screen);
		if (!request.supplementEnabled() && !GenerationIngredients.enoughWithoutSupplement(selected)) {
			trackFailure(userId, screen, mode, regenerated, ErrorCode.NO_INGREDIENTS, start);
			throw new BusinessException(ErrorCode.NO_INGREDIENTS);
		}

		Recipe draft;
		try {
			draft = generateWithRetry(userId, request);
		} catch (BusinessException e) {
			trackFailure(userId, screen, mode, regenerated, e.getErrorCode(), start);
			throw e;
		}

		Saved result;
		try {
			result = transactionTemplate.execute(status -> new Saved(saveRecipePort.save(draft),
					consumeRecipeQuotaUseCase.consumeRecipeQuota(userId)));
		} catch (BusinessException e) {
			// 확인과 저장 사이에 다른 요청이 마지막 횟수를 썼다. 저장은 롤백됐다.
			if (e.getErrorCode() == ErrorCode.GENERATION_LIMIT_EXCEEDED) {
				trackQuotaExceeded(userId, screen);
			}
			throw e;
		}

		Set<Long> expiringIds = request.fridgeIngredients().stream()
				.filter(RecipeAiRequest.FridgeItem::expiringSoon)
				.map(RecipeAiRequest.FridgeItem::ingredientId)
				.collect(Collectors.toSet());
		Recipe saved = result.recipe();
		List<RecipeIngredient> ingredients = saved.ingredients();
		eventTracker.track(EventName.RECIPE_GENERATED, screen, Map.of(
				"recipe_id", saved.id(),
				"recipe_mode", mode,
				"is_regenerated", regenerated,
				"duration_ms", elapsedMillis(start),
				"expiring_used_count", ingredients.stream().filter(i -> expiringIds.contains(i.ingredientId())).count(),
				"shortage_count", ingredients.stream().filter(i -> i.ingredientId() == null).count(),
				"remaining_quota", result.quota().remaining()), EventTrackCommand.ofUser(userId));
		return saved;
	}

	private void requireQuota(Long userId, Screen screen) {
		try {
			consumeRecipeQuotaUseCase.requireRecipeQuota(userId);
		} catch (BusinessException e) {
			if (e.getErrorCode() == ErrorCode.GENERATION_LIMIT_EXCEEDED) {
				trackQuotaExceeded(userId, screen);
			}
			throw e;
		}
	}

	private Recipe generateWithRetry(Long userId, RecipeAiRequest request) {
		BusinessException last = null;
		for (int attempt = 1; attempt <= MAX_AI_ATTEMPTS; attempt++) {
			try {
				GeneratedRecipe generated = aiPort.generate(request);
				return GeneratedRecipeValidator.toRecipe(userId, request, generated);
			} catch (BusinessException e) {
				if (!RETRYABLE.contains(e.getErrorCode())) {
					throw e;
				}
				last = e;
				log.info("AI 레시피 생성 실패({}회차): {}", attempt, e.getErrorCode());
				if (attempt < MAX_AI_ATTEMPTS) {
					pause();
				}
			}
		}
		throw last;
	}

	private void pause() {
		if (retryDelay.isZero()) {
			return;
		}
		try {
			Thread.sleep(retryDelay.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
		}
	}

	private static int servingsOf(User user, GenerateRecipeCommand command) {
		if (command.mode() == RecipeSource.QUICK) {
			Integer household = user.householdSize();
			return household == null || household < 1 ? DEFAULT_SERVINGS : household;
		}
		Integer servings = command.servings();
		if (servings == null || servings < 1 || servings > MAX_SERVINGS) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "식사 인원은 1~" + MAX_SERVINGS + "명입니다.");
		}
		return servings;
	}

	private void trackFailure(Long userId, Screen screen, String mode, boolean regenerated, ErrorCode code,
			long start) {
		eventTracker.track(EventName.RECIPE_GENERATE_FAILED, screen, Map.of(
				"recipe_mode", mode,
				"is_regenerated", regenerated,
				"error_code", code.name(),
				"duration_ms", elapsedMillis(start)), EventTrackCommand.ofUser(userId));
	}

	private void trackQuotaExceeded(Long userId, Screen screen) {
		RecipeQuota quota = getUsageQuotasUseCase.getRecipeQuota(userId);
		int limitTotal = quota.limit() + quota.bonusRemaining();
		int used = quota.used();
		eventTracker.track(EventName.QUOTA_EXCEEDED, screen, Map.of(
				"limit_feature", "RECIPE",
				"limit_total", limitTotal,
				"used_count", used), EventTrackCommand.ofUser(userId));
	}

	private record Saved(Recipe recipe, RecipeQuota quota) {
	}

	private static long elapsedMillis(long startNanos) {
		return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
	}
}
