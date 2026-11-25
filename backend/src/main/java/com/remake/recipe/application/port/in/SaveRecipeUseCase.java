package com.remake.recipe.application.port.in;

/**
 * 레시피 저장·저장 취소·즐겨찾기(API 34~37).
 * 저장은 내가 만든 레시피만 한다(공유 레시피 저장은 공유 영역 API 53). 저장 취소·즐겨찾기는 저장 목록에 있는 레시피면 된다.
 * 없거나 볼 수 없는 레시피는 RECIPE_NOT_FOUND, 볼 수 있지만 저장하지 않았으면 SAVED_RECIPE_NOT_FOUND.
 */
public interface SaveRecipeUseCase {

	/**
	 * @param cartAddedCount 장바구니에 새로 담은 행 수
	 */
	record SaveResult(boolean saved, int cartAddedCount) {
	}

	/** 이미 저장돼 있어도 같은 결과를 돌려준다(그때 장바구니·이벤트는 없음). */
	SaveResult save(Long userId, Long recipeId);

	/** 저장 목록에서만 뺀다. 레시피 원본·공유 글·장바구니는 그대로(#57). */
	void unsave(Long userId, Long recipeId);

	/** 바꾼 뒤의 즐겨찾기 값을 돌려준다. 같은 값으로 다시 보내도 된다. */
	boolean setFavorite(Long userId, Long recipeId, boolean favorite);
}
