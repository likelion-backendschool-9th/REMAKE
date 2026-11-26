// 공유 레시피 상세(p4.1.4.1) 값. 모양은 API 52(GET /user/shared-recipes/{id}) 응답을 따랐다.
// 완전 익명이라 작성자 정보는 없다(domain-rules 7). TODO: API 52를 붙이면 서버 값을 쓴다.

import { type RecipeContent, SAMPLE_RECIPE_CONTENT } from "../../../_lib/recipe";

export type SharedRecipeDetail = RecipeContent & {
  /** 공유 글 ID */
  id: number;
  /** 공유 사진 주소(/api/v1/user/shared-recipe-photos/{id}, 로그인 쿠키 필요) */
  imageUrl: string;
  saveCount: number;
  /** 내가 저장했는지 */
  saved: boolean;
};

/** 신고 사유 4개(API 55 reason). 기타도 글을 쓰지 않는다 */
export const REPORT_REASONS = [
  { value: "OFFENSIVE_IMAGE", label: "불쾌한 이미지 사용" },
  { value: "AD_TITLE", label: "광고성 제목" },
  { value: "DANGEROUS_RECIPE", label: "위험한 조리법 공유" },
  { value: "OTHER", label: "기타" },
] as const;

export type ReportReason = (typeof REPORT_REASONS)[number]["value"];

/** 미리보기용(피그마 p4.1.4.1 문구). 양상추·토마토는 내 냉장고에 없는 재료(빨간 테두리) */
export const SAMPLE_SHARED_RECIPE: SharedRecipeDetail = {
  ...SAMPLE_RECIPE_CONTENT,
  id: 201,
  imageUrl: "/images/recipe/sample-shared-photo.webp",
  ingredients: [
    { name: "양상추", amount: 3, unit: "SHEET", have: false },
    { name: "찬밥", amount: 300, unit: "G", have: true },
    { name: "계란", amount: 1, unit: "EA", have: true },
    { name: "베이컨", amount: 2, unit: "SHEET", have: true },
    { name: "토마토", amount: 1, unit: "EA", have: false },
  ],
  saveCount: 30,
  saved: false,
};
