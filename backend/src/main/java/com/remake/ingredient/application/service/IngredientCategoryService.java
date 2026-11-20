package com.remake.ingredient.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.application.port.out.LoadIngredientCategoryPort;
import com.remake.ingredient.domain.IngredientCategory;

import lombok.RequiredArgsConstructor;

/**
 * UseCase 구현. 자기 기능의 Port만 부르고, 실패는 BusinessException(ErrorCode)을 던지기만 한다.
 * 에러 응답은 GlobalExceptionHandler가 만든다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientCategoryService implements GetIngredientCategoriesUseCase {

	private final LoadIngredientCategoryPort loadIngredientCategoryPort;

	@Override
	public List<IngredientCategory> getIngredientCategories() {
		return loadIngredientCategoryPort.loadAll();
	}

	@Override
	public IngredientCategory getIngredientCategory(Long categoryId) {
		return loadIngredientCategoryPort.loadById(categoryId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INGREDIENT_CATEGORY_NOT_FOUND));
	}
}
