package com.remake.recipe.application.port.out;

/**
 * AI 레시피 생성. 구현은 adapter/out/ai에 있고 식단표 생성도 이 포트를 쓴다.
 * 한 번만 호출하고 재시도는 하지 않는다(재시도는 응답 검증까지 포함해 Service가 한다).
 * 실패하면 BusinessException을 던진다.
 * <ul>
 * <li>제한 시간 초과: AI_TIMEOUT</li>
 * <li>공급자 오류·거절·호출 한도 초과(429)·키 없음: AI_UPSTREAM_ERROR</li>
 * <li>응답을 레시피 모양으로 읽을 수 없음: AI_INVALID_RESPONSE</li>
 * </ul>
 * 공급자의 원래 에러 문구는 예외 메시지에 넣지 않는다(로그에만 남긴다).
 */
public interface GenerateRecipeAiPort {

	GeneratedRecipe generate(RecipeAiRequest request);
}
