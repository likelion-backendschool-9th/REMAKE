package com.remake.ingredient.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.RegisterCartIngredientsUseCase;
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.application.port.in.RegisterIngredientUseCase;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.application.port.out.SaveIngredientPort;
import com.remake.ingredient.domain.ExpiryPolicy;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.receipt.application.port.in.RecordManualIngredientExpenseUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 재료 등록. @Transactional은 호출한 쪽의 트랜잭션에 합류하므로(기본 전파 방식), 영수증 확정에서 부르면
 * 영수증 저장과 같은 트랜잭션이 되어 한 건이라도 실패하면 함께 롤백된다.
 * 도메인 규칙을 어긴 IllegalArgumentException은 INVALID_INPUT으로 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class IngredientRegistrationService implements RegisterIngredientUseCase, RegisterCartIngredientsUseCase {

	private final LoadIngredientCategoryPort loadIngredientCategoryPort;
	private final SaveIngredientPort saveIngredientPort;
	private final RecordManualIngredientExpenseUseCase manualExpenses;
	private final ResolveIngredientUnitUseCase resolveIngredientUnitUseCase;
	private final EventTracker eventTracker;
	private final Clock clock;

	@Override
	@Transactional
	public Long registerIngredient(RegisterIngredientCommand command) {
		return register(command, LocalDateTime.now(clock), false).id();
	}

	@Override
	@Transactional
	public List<Long> registerIngredients(List<RegisterIngredientCommand> commands) {
		if (commands == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "등록할 재료가 없습니다.");
		}
		LocalDateTime registeredAt = LocalDateTime.now(clock);
		return commands.stream()
				.map(command -> register(command, registeredAt, false).id())
				.toList();
	}

	@Override
	@Transactional
	public List<Long> registerCartIngredients(List<RegisterIngredientCommand> commands) {
		if (commands == null || commands.stream().anyMatch(c -> c == null || c.unit() == null
				|| c.receiptItemId() != null || c.price() != null)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		LocalDateTime registeredAt = LocalDateTime.now(clock);
		return commands.stream().map(command -> register(command, registeredAt, true).id()).toList();
	}

	private Ingredient register(RegisterIngredientCommand command, LocalDateTime registeredAt, boolean fromCart) {
		if (command == null || command.categoryCode() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "카테고리는 필수입니다.");
		}
		IngredientCategory category = loadIngredientCategoryPort.loadByCode(command.categoryCode())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));

		LocalDate expirationDate = command.expirationDate();
		if (expirationDate == null) {
			// 소비기한을 모르면 기준일(영수증은 구매일, 없으면 오늘)에 카테고리 기본 보관 일수를 더해 추정한다.
			LocalDate baseDate = command.expirationBaseDate() != null ? command.expirationBaseDate() : registeredAt.toLocalDate();
			expirationDate = ExpiryPolicy.estimateExpirationDate(baseDate, category.defaultShelfLifeDays());
		}

		// 단위는 서버가 정한다. 호출한 기능이 정해서 넘긴 단위(영수증 PACK 예외 등)가 있으면 그대로 쓴다.
		IngredientUnit unit = command.unit() != null ? command.unit()
				: resolveIngredientUnitUseCase.resolveUnit(command.name(), command.categoryCode());

		try {
			Ingredient ingredient = Ingredient.register(command.userId(), category.id(), command.receiptItemId(),
					command.name(), command.storage(), expirationDate, command.amount(), unit,
					command.price());
			Ingredient saved = saveIngredientPort.save(ingredient);
			// 영수증 재료는 영수증 삭제 후에도 직접 등록 지출로 취급하지 않는다.
			if (command.receiptItemId() == null) {
				manualExpenses.record(saved, registeredAt);
			}
			trackAdded(saved, category, command.receiptItemId() != null, fromCart);
			return saved;
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
	}

	/**
	 * ingredient_added 이벤트. EventTracker가 커밋 뒤에 보내므로 롤백되면 나가지 않고, 실패해도 등록은 깨지지 않는다.
	 * 개인정보·자유 입력 없이 코드 값과 true/false만 넣는다. 이름은 넣지 않는다.
	 * add_source는 정의서 소문자(manual / receipt / cart). storage는 정의서대로 대문자(FRIDGE 등).
	 * duration_sec(추가 화면에 머문 시간)은 서버가 알 수 없어 보내지 않는다.
	 */
	private void trackAdded(Ingredient ingredient, IngredientCategory category, boolean fromReceipt, boolean fromCart) {
		eventTracker.track(EventName.INGREDIENT_ADDED, fromCart ? Screen.CART_LIST : fromReceipt ? Screen.RECEIPT_RESULT : Screen.INGREDIENT_ADD,
				Map.of("add_source", fromCart ? "cart" : fromReceipt ? "receipt" : "manual",
						"category_code", category.code().name(),
						"storage", ingredient.storage().name(),
						"has_price", ingredient.price() != null),
				EventTrackCommand.ofUser(ingredient.userId()));
	}
}
