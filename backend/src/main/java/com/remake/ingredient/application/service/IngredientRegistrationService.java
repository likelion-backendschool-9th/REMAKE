package com.remake.ingredient.application.service;

import java.time.Clock;
import java.time.LocalDate;
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
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.application.port.in.RegisterIngredientUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.application.port.out.SaveIngredientPort;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.ExpiryPolicy;

import lombok.RequiredArgsConstructor;

/**
 * 재료 등록. @Transactional은 호출한 쪽의 트랜잭션에 합류하므로(기본 전파 방식), 영수증 확정에서 부르면
 * 영수증 저장과 같은 트랜잭션이 되어 한 건이라도 실패하면 함께 롤백된다.
 * 도메인 규칙을 어긴 IllegalArgumentException은 INVALID_INPUT으로 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class IngredientRegistrationService implements RegisterIngredientUseCase {

	private final LoadIngredientCategoryPort loadIngredientCategoryPort;
	private final SaveIngredientPort saveIngredientPort;
	private final EventTracker eventTracker;
	private final Clock clock;

	@Override
	@Transactional
	public Long registerIngredient(RegisterIngredientCommand command) {
		return register(command, LocalDate.now(clock)).id();
	}

	@Override
	@Transactional
	public List<Long> registerIngredients(List<RegisterIngredientCommand> commands) {
		if (commands == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "등록할 재료가 없습니다.");
		}
		LocalDate today = LocalDate.now(clock);
		return commands.stream()
				.map(command -> register(command, today).id())
				.toList();
	}

	private Ingredient register(RegisterIngredientCommand command, LocalDate today) {
		if (command == null || command.categoryCode() == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "카테고리는 필수입니다.");
		}
		IngredientCategory category = loadIngredientCategoryPort.loadByCode(command.categoryCode())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));

		LocalDate expirationDate = command.expirationDate();
		if (expirationDate == null) {
			// 소비기한을 모르면 기준일(영수증은 구매일, 없으면 오늘)에 카테고리 기본 보관 일수를 더해 추정한다.
			LocalDate baseDate = command.expirationBaseDate() != null ? command.expirationBaseDate() : today;
			expirationDate = ExpiryPolicy.estimateExpirationDate(baseDate, category.defaultShelfLifeDays());
		}

		try {
			Ingredient ingredient = Ingredient.register(command.userId(), category.id(), command.receiptItemId(),
					command.name(), command.storage(), expirationDate, command.amount(), command.unit(),
					command.price());
			Ingredient saved = saveIngredientPort.save(ingredient);
			trackAdded(saved, category, command.receiptItemId() != null);
			return saved;
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
	}

	/**
	 * ingredient_added 이벤트. EventTracker가 커밋 뒤에 보내므로 롤백되면 나가지 않고, 실패해도 등록은 깨지지 않는다.
	 * 개인정보·자유 입력 없이 코드 값과 true/false만 넣는다. 이름은 넣지 않는다.
	 * add_source는 영수증 품목에서 온 것이면 RECEIPT, 아니면 MANUAL이다(카탈로그 선택 구분 CATALOG는 아직 보내지 않는다).
	 * duration_sec(추가 화면에 머문 시간)은 서버가 알 수 없어 보내지 않는다.
	 */
	private void trackAdded(Ingredient ingredient, IngredientCategory category, boolean fromReceipt) {
		eventTracker.track(EventName.INGREDIENT_ADDED, fromReceipt ? Screen.RECEIPT_RESULT : Screen.INGREDIENT_ADD,
				Map.of("add_source", fromReceipt ? "RECEIPT" : "MANUAL",
						"category_code", category.code().name(),
						"storage", ingredient.storage().name(),
						"has_price", ingredient.price() != null),
				EventTrackCommand.ofUser(ingredient.userId()));
	}
}
