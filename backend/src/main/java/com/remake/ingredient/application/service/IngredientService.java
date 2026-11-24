package com.remake.ingredient.application.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.DeleteIngredientUseCase;
import com.remake.ingredient.application.port.in.GetIngredientUseCase;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.ingredient.application.port.in.UpdateIngredientCommand;
import com.remake.ingredient.application.port.in.UpdateIngredientUseCase;
import com.remake.ingredient.application.port.out.DeleteIngredientPort;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.application.port.out.LoadIngredientLogPort;
import com.remake.ingredient.application.port.out.LoadIngredientPort;
import com.remake.ingredient.application.port.out.SaveIngredientPort;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;

import lombok.RequiredArgsConstructor;

/**
 * 재료 상세 조회·수정·삭제. 다른 사용자의 재료는 "없음"과 같게 INGREDIENT_NOT_FOUND(404)로 답한다.
 * 도메인 규칙을 어긴 IllegalArgumentException은 INVALID_INPUT으로 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class IngredientService implements GetIngredientUseCase, UpdateIngredientUseCase, DeleteIngredientUseCase {

	private final LoadIngredientPort loadIngredientPort;
	private final LoadIngredientCategoryPort loadIngredientCategoryPort;
	private final SaveIngredientPort saveIngredientPort;
	private final DeleteIngredientPort deleteIngredientPort;
	private final LoadIngredientLogPort loadIngredientLogPort;
	private final ResolveIngredientUnitUseCase resolveIngredientUnitUseCase;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public FridgeIngredient getIngredient(Long userId, Long ingredientId) {
		return toFridgeIngredient(loadOwned(userId, ingredientId));
	}

	@Override
	@Transactional
	public FridgeIngredient updateIngredient(UpdateIngredientCommand command) {
		Ingredient ingredient = loadOwned(command.userId(), command.ingredientId());
		// 수량 수정은 입력 실수를 바로잡는 용도라, 쓰거나 버린 기록이 있으면 처음 양이 달라지므로 거부한다.
		if (command.amount() != null && loadIngredientLogPort.existsByIngredientId(ingredient.id())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "사용하거나 폐기한 기록이 있는 재료는 수량을 고칠 수 없습니다.");
		}
		try {
			boolean nameOrCategoryChanged = false;
			if (command.name() != null) {
				String before = ingredient.name();
				ingredient.changeName(command.name());
				nameOrCategoryChanged |= !before.equals(ingredient.name());
			}
			if (command.categoryCode() != null) {
				Long before = ingredient.categoryId();
				ingredient.changeCategory(loadIngredientCategoryPort.loadByCode(command.categoryCode())
						.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND)).id());
				nameOrCategoryChanged |= !before.equals(ingredient.categoryId());
			}
			if (nameOrCategoryChanged) {
				// 이름이나 카테고리가 바뀌면 단위만 다시 정한다. 수량 숫자는 그대로다(대파 1단 → 양파 1개).
				// 기록이 있어도 같고, 기록은 당시 이름·단위를 따로 갖고 있어 지난 기록은 바뀌지 않는다.
				ingredient.changeUnit(
						resolveIngredientUnitUseCase.resolveUnit(ingredient.name(), categoryCodeOf(ingredient)));
			}
			if (command.amount() != null) {
				ingredient.changeAmount(command.amount());
			}
			if (command.expirationDate() != null) {
				ingredient.changeExpirationDate(command.expirationDate());
			}
			if (command.storage() != null) {
				ingredient.changeStorage(command.storage());
			}
			if (command.price() != null) {
				ingredient.changePrice(command.price().value());
			}
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, e.getMessage());
		}
		return toFridgeIngredient(saveIngredientPort.save(ingredient));
	}

	@Override
	@Transactional
	public void deleteIngredient(Long userId, Long ingredientId) {
		Ingredient ingredient = loadOwned(userId, ingredientId);
		// 사용·폐기 기록이 있어도 지울 수 있다. 기록은 이름·단위와 함께 남고 재료 연결만 DB가 비운다(ON DELETE SET NULL).
		deleteIngredientPort.deleteById(ingredient.id());
	}

	// 카테고리 이미지와 오늘 기준 D-day·상태를 붙인다. 계산은 FridgeIngredient.of 한 곳이라 목록(API 12)과 같다.
	private FridgeIngredient toFridgeIngredient(Ingredient ingredient) {
		IngredientCategory category = loadIngredientCategoryPort.loadById(ingredient.categoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));
		return FridgeIngredient.of(ingredient, category, LocalDate.now(clock));
	}

	private IngredientCategoryCode categoryCodeOf(Ingredient ingredient) {
		return loadIngredientCategoryPort.loadById(ingredient.categoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND)).code();
	}

	private Ingredient loadOwned(Long userId, Long ingredientId) {
		return loadIngredientPort.loadByIdAndUserId(ingredientId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
	}
}
