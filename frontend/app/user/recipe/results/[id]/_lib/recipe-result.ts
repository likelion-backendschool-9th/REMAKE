// 레시피 생성 결과(p4.1.3) 값. API 28·29 응답은 상세(API 30)와 같은 모양이다.
// 생성·다시 받기 응답은 lib/queries/recipes.ts가 상세 캐시에 넣고 이 주소로 이동한다.

import type { RecipeDetailData } from "@/lib/queries/recipes";

import { type RecipeContent, SAMPLE_RECIPE_CONTENT } from "../../../_lib/recipe";

export type RecipeResult = RecipeContent & {
  id: number;
  /** 저장했는지. 만든 직후에는 false(저장 전까지 목록에 안 보임) */
  saved: boolean;
};

/** 상세 응답(API 28·29·30)을 결과 화면 값으로 바꾼다 */
export function toRecipeResult(data: RecipeDetailData): RecipeResult {
  return {
    id: data.id,
    title: data.title,
    servings: data.servings,
    cookMinutes: data.cookMinutes,
    tips: data.tips ?? null,
    ingredients: data.ingredients.map(({ name, amount, unit }) => ({
      name,
      amount: amount ?? null,
      unit,
    })),
    steps: data.steps,
    saved: data.saved,
  };
}

/** 미리보기용(피그마 p4.1.3 문구) */
export const SAMPLE_RECIPE_RESULT: RecipeResult = {
  ...SAMPLE_RECIPE_CONTENT,
  id: 41,
  saved: false,
};
