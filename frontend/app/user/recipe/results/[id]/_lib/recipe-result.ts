// 레시피 생성 결과(p4.1.3) 값. API 28·29 응답은 상세(API 30)와 같은 모양이다.
// TODO: 생성 요청 응답을 이 화면에 넘기는 방법(캐시에 넣고 주소 이동)은 API를 붙일 때 정한다.

import { type RecipeContent, SAMPLE_RECIPE_CONTENT } from "../../../_lib/recipe";

export type RecipeResult = RecipeContent & {
  id: number;
  /** 저장했는지. 만든 직후에는 false(저장 전까지 목록에 안 보임) */
  saved: boolean;
};

/** 미리보기용(피그마 p4.1.3 문구) */
export const SAMPLE_RECIPE_RESULT: RecipeResult = {
  ...SAMPLE_RECIPE_CONTENT,
  id: 41,
  saved: false,
};
