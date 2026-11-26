// 레시피 메인(p4.1) 값. 모양은 API 49(금주의 레시피 TOP 3)·API 33(저장한 레시피) 응답을 따랐다.
// TODO: API를 붙이면 서버 값을 쓴다. 순위·재료 요약·바로 가능 여부는 서버가 정해 준다.

/** 금주의 레시피 카드 한 장(API 49). 순위 변동은 보여 주지 않는다(domain-rules 7) */
export type WeeklyTopItem = {
  rank: number;
  /** 공유 글 ID */
  id: number;
  title: string;
  /** 공유 사진 주소(/api/v1/user/shared-recipe-photos/{id}, 로그인 쿠키 필요) */
  imageUrl: string;
  ingredientSummary: string;
  extraIngredientCount: number;
};

export type SavedRecipeFilter = "ALL" | "FAVORITE" | "AVAILABLE";
export type SavedRecipeSort = "SAVED_DESC" | "SAVED_ASC";

/** 저장한 레시피 한 줄(API 33 content[]) */
export type SavedRecipeRow = {
  recipeId: number;
  title: string;
  ingredientSummary: string;
  extraIngredientCount: number;
  favorite: boolean;
};

/** 저장한 레시피 목록(API 33). 페이지 값(page·size·hasNext)은 무한 스크롤을 붙일 때 쓴다 */
export type SavedRecipeList = {
  /** 저장한 레시피 전체 개수("저장한 레시피 N") */
  totalCount: number;
  content: SavedRecipeRow[];
};

/** 칩 순서와 문구(피그마 p4.1). 바로 가능 = 지금 냉장고 재료로 만들 수 있음(서버가 거름) */
export const SAVED_RECIPE_FILTERS: readonly { value: SavedRecipeFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "FAVORITE", label: "즐겨찾기" },
  { value: "AVAILABLE", label: "바로 가능" },
];

export const SAVED_RECIPE_SORT_LABEL: Record<SavedRecipeSort, string> = {
  SAVED_DESC: "최신 저장순",
  SAVED_ASC: "오래된 저장순",
};

/** 미리보기용(피그마 p4.1 문구). 실제 라우트에서는 쓰지 않는다 */
export const SAMPLE_WEEKLY_TOP: WeeklyTopItem[] = [
  {
    rank: 1,
    id: 101,
    title: "핫 치킨 치즈 포케",
    imageUrl: "/images/recipe/sample-poke.webp",
    ingredientSummary: "미역 · 냉동 만두",
    extraIngredientCount: 3,
  },
  {
    rank: 2,
    id: 102,
    title: "계란 올리브 샌드위치",
    imageUrl: "/images/recipe/sample-sandwich.webp",
    ingredientSummary: "토스트 · 계란",
    extraIngredientCount: 3,
  },
  {
    rank: 3,
    id: 103,
    title: "오픈! 전남친 토스트",
    imageUrl: "/images/recipe/sample-toast.webp",
    ingredientSummary: "토스트 · 계란",
    extraIngredientCount: 3,
  },
];

/** 미리보기용(피그마 p4.1). 재료 요약은 서버 규칙대로 앞 3개 + 나머지 개수 */
export const SAMPLE_SAVED_RECIPES: SavedRecipeList = {
  totalCount: 3,
  content: [
    {
      recipeId: 31,
      title: "떡만두 미역국",
      ingredientSummary: "미역 · 냉동 만두 · 계란",
      extraIngredientCount: 1,
      favorite: true,
    },
    {
      recipeId: 32,
      title: "양송이 크림 파스타",
      ingredientSummary: "양송이 · 생크림 · 파스타면",
      extraIngredientCount: 2,
      favorite: false,
    },
    {
      recipeId: 33,
      title: "고추장 양배추 찜",
      ingredientSummary: "양배추 · 고추장 · 돼지고기",
      extraIngredientCount: 1,
      favorite: false,
    },
  ],
};
