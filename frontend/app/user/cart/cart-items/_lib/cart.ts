// 장바구니(p7) 값. 모양은 API 45(GET /user/cart-items) 응답 items 한 줄에서 화면이 쓰는 것만 따랐다.
// TODO: API 45를 붙이면 서버 값을 쓴다(페이징 없음). 그림은 서버 imageUrl 대신 카테고리 기본 이미지를 쓴다.

import { z } from "zod";

import type { CategoryCode, RecipeUnit } from "@/lib/ingredients";

export type CartItem = {
  id: number;
  /** 이 재료를 담게 한 레시피 이름(같은 재료라도 레시피가 다르면 따로 담긴다) */
  recipeTitle: string;
  name: string;
  categoryCode: CategoryCode;
  /** 없으면 "약간"(TO_TASTE) */
  amount: number | null;
  unit: RecipeUnit;
};

/** 수량 최대값: 백엔드 numeric(10,3)과 같은 범위(정수 7자리, 1천만 미만) */
export const CART_AMOUNT_MAX = 9_999_999;

/** 수량 변경 입력: 숫자와 소수만(분수 없음, 소수 3자리까지, domain-rules "Units") */
export const cartAmountSchema = z.object({
  amount: z
    .string()
    .regex(/^\d+(\.\d{1,3})?$/, "숫자만 작성해 주세요(소수는 3자리까지)")
    .refine((value) => Number(value) > 0, "0보다 큰 수량을 작성해 주세요")
    .refine((value) => Number(value) <= CART_AMOUNT_MAX, "수량이 너무 커요"),
});

export type CartAmountForm = z.infer<typeof cartAmountSchema>;

/** 마크업 확인용 값(피그마 p7-장바구니-항목 선택과 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_CART_ITEMS: CartItem[] = [
  {
    id: 1,
    recipeTitle: "간장 목살 리조또",
    name: "목살",
    categoryCode: "MEAT",
    amount: 600,
    unit: "G",
  },
  {
    id: 2,
    recipeTitle: "돼지 목살 샌드위치",
    name: "목살",
    categoryCode: "MEAT",
    amount: 200,
    unit: "G",
  },
  {
    id: 3,
    recipeTitle: "명란마요 오꼬노미야끼",
    name: "양상추",
    categoryCode: "VEGETABLE",
    amount: 1,
    unit: "EA",
  },
  {
    id: 4,
    recipeTitle: "명란마요 오꼬노미야끼",
    name: "마요네즈",
    categoryCode: "SEASONING",
    amount: 2,
    unit: "TBSP",
  },
  {
    id: 5,
    recipeTitle: "유자 블루베리 스무디",
    name: "블루베리",
    categoryCode: "FRUIT",
    amount: 200,
    unit: "G",
  },
];
