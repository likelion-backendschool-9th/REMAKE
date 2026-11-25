// 냉장고 재료 추가(p2.2)·수정(p2.1) 마크업 확인용 값. 미리보기(app/dev)에서만 쓴다.
// 입력 값 모양과 계산은 공용 lib/ingredient-form.ts에 있다.

import { type IngredientDetail, type IngredientDraft, toDraft } from "@/lib/ingredient-form";

/** 마크업 확인용 값(피그마 p2.1-재료 수정 위 요약 줄과 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_DETAIL: IngredientDetail = {
  id: 1,
  name: "목살",
  categoryCode: "MEAT",
  storage: "FREEZER",
  expirationDate: "2026-12-21",
  price: 14500,
  amount: 500,
  unit: "G",
};

/** 피그마 p2.1 카드처럼 값을 바꾼 상태(재료명 우둔살, 수량 600g) */
export const SAMPLE_EDITED_DRAFT: IngredientDraft = {
  ...toDraft(SAMPLE_DETAIL),
  name: "우둔살",
  amount: 600,
};

/** 재료 추가에서 모두 채운 상태(버튼 켜짐 확인용) */
export const SAMPLE_FILLED_DRAFT: IngredientDraft = {
  name: "쪽파",
  categoryCode: "VEGETABLE",
  storage: "FRIDGE",
  expirationDate: "2026-11-27",
  price: null,
  amount: 1,
  unit: "BUNCH",
};
