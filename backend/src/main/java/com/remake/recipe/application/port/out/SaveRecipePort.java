package com.remake.recipe.application.port.out;

import com.remake.recipe.domain.Recipe;

/** 새 레시피를 재료·단계와 함께 저장한다. 레시피는 고치지 않으므로 새로 저장만 한다. id가 채워진 레시피를 돌려준다. */
public interface SaveRecipePort {

	Recipe save(Recipe recipe);
}
