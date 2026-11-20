package com.remake.ingredient.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.DeleteIngredientUseCase;
import com.remake.ingredient.application.port.in.GetIngredientUseCase;
import com.remake.ingredient.application.port.in.UpdateIngredientCommand;
import com.remake.ingredient.application.port.in.UpdateIngredientUseCase;
import com.remake.ingredient.application.port.out.DeleteIngredientPort;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.application.port.out.LoadIngredientPort;
import com.remake.ingredient.application.port.out.SaveIngredientPort;
import com.remake.ingredient.domain.Ingredient;

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

	@Override
	@Transactional(readOnly = true)
	public Ingredient getIngredient(Long userId, Long ingredientId) {
		return loadOwned(userId, ingredientId);
	}

	@Override
	@Transactional
	public Ingredient updateIngredient(UpdateIngredientCommand command) {
		Ingredient ingredient = loadOwned(command.userId(), command.ingredientId());
		try {
			if (command.name() != null) {
				ingredient.changeName(command.name());
			}
			if (command.categoryCode() != null) {
				ingredient.changeCategory(loadIngredientCategoryPort.loadByCode(command.categoryCode())
						.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND)).id());
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
		return saveIngredientPort.save(ingredient);
	}

	@Override
	@Transactional
	public void deleteIngredient(Long userId, Long ingredientId) {
		Ingredient ingredient = loadOwned(userId, ingredientId);
		deleteIngredientPort.deleteById(ingredient.id());
	}

	private Ingredient loadOwned(Long userId, Long ingredientId) {
		return loadIngredientPort.loadByIdAndUserId(ingredientId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND));
	}
}
