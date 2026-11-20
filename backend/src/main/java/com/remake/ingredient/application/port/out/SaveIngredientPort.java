package com.remake.ingredient.application.port.out;

import com.remake.ingredient.domain.Ingredient;

/** 재료 저장. id가 없으면 새로 저장하고, 있으면 바뀐 값을 고친다. id가 채워진 재료를 돌려준다. */
public interface SaveIngredientPort {

	Ingredient save(Ingredient ingredient);
}
