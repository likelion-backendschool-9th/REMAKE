package com.remake.ingredient.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.ConsumeIngredientCommand;
import com.remake.ingredient.application.port.in.ConsumeIngredientResult;
import com.remake.ingredient.application.port.in.ConsumeIngredientUseCase;
import com.remake.ingredient.application.port.out.DeleteIngredientPort;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.application.port.out.LoadIngredientPort;
import com.remake.ingredient.application.port.out.SaveIngredientLogPort;
import com.remake.ingredient.application.port.out.SaveIngredientPort;
import com.remake.ingredient.domain.ConsumeVia;
import com.remake.ingredient.domain.ExpiryPolicy;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientLog;
import com.remake.ingredient.domain.IngredientLogType;
import com.remake.ingredient.domain.InsufficientAmountException;

import lombok.RequiredArgsConstructor;

/**
 * 재료 사용·폐기. 재료의 남은 양을 줄이고 기록을 남기는 일을 한 트랜잭션에서 한다. 남은 양이 0이 되면 재료 행을 지운다.
 * 호출한 쪽의 트랜잭션에 합류하므로(기본 전파 방식) 요리 후 차감(#77)에서 부르면 요리 차감과 함께 성공하거나 롤백된다.
 * 같은 재료의 동시 요청은 재료 행 잠금(로드 시 FOR UPDATE)으로 차례로 처리한다. 늦은 요청은 앞 요청이 뺀 뒤의 양을 보고, 이미 지워졌으면 INGREDIENT_NOT_FOUND이다.
 * ingredient_used·ingredient_discarded 이벤트는 EventTracker로 커밋 뒤에 보낸다(롤백되면 나가지 않고, 실패해도 이 일은 깨지지 않는다).
 */
@Service
@RequiredArgsConstructor
public class IngredientConsumeService implements ConsumeIngredientUseCase {

	private final LoadIngredientPort loadIngredientPort;
	private final LoadIngredientCategoryPort loadIngredientCategoryPort;
	private final SaveIngredientPort saveIngredientPort;
	private final DeleteIngredientPort deleteIngredientPort;
	private final SaveIngredientLogPort saveIngredientLogPort;
	private final EventTracker eventTracker;
	private final Clock clock;

	@Override
	@Transactional
	public ConsumeIngredientResult consumeIngredient(ConsumeIngredientCommand command) {
		if (command == null || command.type() == null || command.via() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "사용 또는 폐기를 선택해야 합니다.");
		}
		if (command.via() == ConsumeVia.COOK && command.type() != IngredientLogType.USED) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "요리 후 차감은 사용 기록만 남길 수 있습니다.");
		}
		Ingredient ingredient = loadIngredientPort.loadByIdAndUserIdForUpdate(command.ingredientId(), command.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
		// 이벤트의 소비기한 상태와 금액은 차감하기 전 값으로 계산한다.
		long dDay = ExpiryPolicy.dDay(ingredient.expirationDate(), LocalDate.now(clock));
		BigDecimal amount = command.amount();
		try {
			ingredient.consume(amount);
			// 처음 양은 차감으로 바뀌지 않으므로 차감 뒤에 계산해도 같다. 폐기일 때만 금액을 남긴다.
			Long costAmount = command.type() == IngredientLogType.DISCARDED ? ingredient.proportionalCost(amount) : null;
			boolean deleted = ingredient.isEmpty();
			// 다 썼으면 재료 행을 지우고 기록은 재료 연결 없이(ingredient_id null) 이름·단위와 함께 남긴다.
			if (deleted) {
				deleteIngredientPort.deleteById(ingredient.id());
			} else {
				saveIngredientPort.save(ingredient);
			}
			saveIngredientLogPort.save(new IngredientLog(null, ingredient.userId(), deleted ? null : ingredient.id(),
					ingredient.categoryId(), command.type(), ingredient.name(), amount, ingredient.unit(), costAmount));
			trackEvent(command, ingredient, dDay, costAmount);
			return new ConsumeIngredientResult(ingredient.amount(), deleted, costAmount);
		// 아래 예외는 IllegalArgumentException의 하위 클래스라 반드시 IllegalArgumentException보다 먼저 잡는다(순서가 바뀌면 INVALID_INPUT이 된다).
		} catch (InsufficientAmountException e) {
			throw new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY, ErrorCode.INSUFFICIENT_QUANTITY.getMessage(),
					Map.of("remaining", e.remaining()));
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
	}

	/**
	 * 개인정보·자유 입력 없이 코드 값과 숫자만 넣는다(이름은 넣지 않는다).
	 * used_ratio는 이번에 뺀 양이 처음 양에서 차지하는 비율(0~1, 소수 둘째 자리)이다.
	 * use_via는 요리 후 차감이면 cook, 냉장고에서 직접 기록하면 manual이다(이벤트 정의서 값, 사용 이벤트에만 있다).
	 * expiry_status는 정의서대로 danger/soon/ok 소문자이다(냉장고 목록 응답의 status는 대문자 그대로).
	 */
	private void trackEvent(ConsumeIngredientCommand command, Ingredient saved, long dDay, Long costAmount) {
		IngredientCategory category = loadIngredientCategoryPort.loadById(saved.categoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));
		Map<String, Object> properties = new HashMap<>();
		properties.put("category_code", category.code().name());
		properties.put("expiry_status", ExpiryPolicy.statusOf(dDay).name().toLowerCase(Locale.ROOT));
		properties.put("used_ratio", command.amount().divide(saved.initialAmount(), 2, RoundingMode.HALF_UP).doubleValue());
		if (command.type() == IngredientLogType.USED) {
			properties.put("use_via", command.via() == ConsumeVia.COOK ? "cook" : "manual");
			eventTracker.track(EventName.INGREDIENT_USED,
					command.via() == ConsumeVia.COOK ? Screen.COOK_DEDUCT : Screen.FRIDGE_LIST, properties,
					EventTrackCommand.ofUser(saved.userId()));
		} else {
			properties.put("has_price", costAmount != null);
			if (costAmount != null) {
				properties.put("cost_amount", costAmount);
			}
			eventTracker.track(EventName.INGREDIENT_DISCARDED, Screen.FRIDGE_LIST, properties,
					EventTrackCommand.ofUser(saved.userId()));
		}
	}
}
