// 내 레시피 상세(p4.1.1) 값. 모양은 API 30(GET /user/recipes/{id}) 응답을 따랐다.
// TODO: API 30을 붙이면 서버 값을 쓴다.

import { type RecipeContent, SAMPLE_RECIPE_CONTENT } from "../../../_lib/recipe";

export type RecipeDetail = RecipeContent & {
  id: number;
  saved: boolean;
  favorite: boolean;
  /** 이번 주(월요일 시작)에 아직 공유하지 않음. false면 ⋮ "공유하기"를 막는다 */
  shareable: boolean;
};

/** 미리보기용(피그마 p4.1.1 문구) */
export const SAMPLE_RECIPE_DETAIL: RecipeDetail = {
  ...SAMPLE_RECIPE_CONTENT,
  id: 31,
  saved: true,
  favorite: false,
  shareable: true,
};
