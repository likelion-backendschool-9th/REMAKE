// 레시피 사용 후 재료 차감(p4.1.1.2) 값. 모양은 API 31(GET /user/recipes/{id}/cook-preview) 응답을 따랐다.
// TODO: API 31을 붙이면 서버 값을 쓴다. 레시피 재료와 같은 이름의 내 냉장고 재료(소비기한 급한 것)를 서버가 짝지어 준다.

import {
  type CategoryCode,
  formatAmount,
  type FridgeUnit,
  getCategoryImage,
} from "@/lib/ingredients";

/** 차감할 재료 한 줄. amount·unit은 냉장고 단위로 바꾼 레시피 양(남은 양까지). 종류가 달라 못 바꾸면 amount null(사용자가 정함) */
export type CookItem = {
  ingredientId: number;
  name: string;
  imageUrl: string;
  amount: number | null;
  unit: FridgeUnit;
  remainingAmount: number;
};

export type CookPreview = { items: CookItem[] };

/** 차감할 양 표시. 양을 아직 모르면 "—" */
export function formatCookAmount(item: CookItem): string {
  return item.amount === null ? "—" : formatAmount(item.amount, item.unit);
}

function sample(
  ingredientId: number,
  name: string,
  category: CategoryCode,
  amount: number,
  unit: FridgeUnit,
): CookItem {
  return {
    ingredientId,
    name,
    imageUrl: getCategoryImage(category),
    amount,
    unit,
    remainingAmount: amount,
  };
}

/** 미리보기용(피그마 p4.1.1.2 문구) */
export const SAMPLE_COOK_PREVIEW: CookPreview = {
  items: [
    sample(11, "목살", "MEAT", 600, "G"),
    sample(12, "쪽파", "VEGETABLE", 1, "BUNCH"),
    sample(13, "굴소스", "SEASONING", 50, "ML"),
  ],
};
