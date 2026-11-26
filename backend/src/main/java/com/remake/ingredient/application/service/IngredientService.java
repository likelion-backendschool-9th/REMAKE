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
import com.remake.ingredient.domain.IngredientDetail;

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
	public IngredientDetail getIngredient(Long userId, Long ingredientId) {
		return toDetail(loadIngredientPort.loadByIdAndUserId(ingredientId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND)));
	}

	@Override
	@Transactional
	public IngredientDetail updateIngredient(UpdateIngredientCommand command) {
		Ingredient ingredient = loadOwned(command.userId(), command.ingredientId());
		// 수량 수정은 입력 실수를 바로잡는 용도라, 쓰거나 버린 기록이 있으면 처음 양이 달라지므로 거부한다.
		if (command.amount() != null && loadIngredientLogPort.existsByIngredientId(ingredient.id())) {
			throw new BusinessException(ErrorCode.INVALID_INPUT, "사용하거나 폐기한 기록이 있는 재료는 수량을 고칠 수 없습니다.");
		}
		try {
			// 단위를 다시 정할지는 수정하기 전 이름·카테고리의 사전 단위와 지금 단위가 같은지로 가른다(아래 참고).
			boolean followedDictionary = unitFollowsName(ingredient);
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
			if (nameOrCategoryChanged && followedDictionary) {
				// 이름이나 카테고리가 바뀌면 단위만 다시 정한다. 수량 숫자는 그대로다(대파 1단 → 양파 1개).
				// 장바구니·영수증 예외로 사전과 다른 단위를 가진 재료(followedDictionary가 false)는 단위와 수량을 그대로 둔다(#233).
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
		return toDetail(saveIngredientPort.save(ingredient));
	}

	@Override
	@Transactional
	public void deleteIngredient(Long userId, Long ingredientId) {
		Ingredient ingredient = loadOwned(userId, ingredientId);
		// 사용·폐기 기록이 있어도 지울 수 있다. 기록은 이름·단위와 함께 남고 재료 연결만 DB가 비운다(ON DELETE SET NULL).
		deleteIngredientPort.deleteById(ingredient.id());
	}

	// 카테고리 이미지와 오늘 기준 D-day·상태를 붙이고, 단위를 다시 정하는 재료인지 더한다. D-day·상태 계산은 FridgeIngredient.of 한 곳이라 목록(API 12)과 같다.
	private IngredientDetail toDetail(Ingredient ingredient) {
		IngredientCategory category = loadIngredientCategoryPort.loadById(ingredient.categoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));
		return new IngredientDetail(FridgeIngredient.of(ingredient, category, LocalDate.now(clock)),
				unitFollowsName(ingredient, category.code()));
	}

	// 지금 단위가 지금 이름·카테고리의 사전 단위와 같으면 true. 사전이 바뀌어 달라졌다면 false가 되어 단위를 유지한다.
	private boolean unitFollowsName(Ingredient ingredient) {
		return unitFollowsName(ingredient, categoryCodeOf(ingredient));
	}

	private boolean unitFollowsName(Ingredient ingredient, IngredientCategoryCode categoryCode) {
		return ingredient.unit() == resolveIngredientUnitUseCase.resolveUnit(ingredient.name(), categoryCode);
	}

	private IngredientCategoryCode categoryCodeOf(Ingredient ingredient) {
		return loadIngredientCategoryPort.loadById(ingredient.categoryId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND)).code();
	}

	// 수정·삭제용이라 행을 잠그고 읽는다(같은 재료의 동시 사용·폐기와 차례로 처리).
	private Ingredient loadOwned(Long userId, Long ingredientId) {
		return loadIngredientPort.loadByIdAndUserIdForUpdate(ingredientId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
	}
}
