// 커스텀 레시피(p4.1.2) 값. 고르기만 하고 직접 쓰는 칸은 없다(AGENTS.md: AI에 자유 입력 금지).
// 선택지 코드는 API 28 요청 값(api-summary "Custom option codes"), 이름은 피그마 p4.1.2 시트 문구.

import type { ExpiryStatus } from "@/components/ExpiryLabel/ExpiryLabel";
import type { AllergenCode } from "@/lib/allergens";
import type { CategoryCode, FridgeUnit, StorageCode } from "@/lib/ingredients";

type Option<T extends string> = { value: T; label: string };

export const TASTES = [
  { value: "MILD", label: "담백한 맛" },
  { value: "SPICY", label: "매콤한 맛" },
  { value: "SWEET", label: "달콤한 맛" },
  { value: "SALTY", label: "짭짤한 맛" },
  { value: "SOUR", label: "새콤한 맛" },
  { value: "NUTTY", label: "고소한 맛" },
] as const satisfies readonly Option<string>[];

export const COOK_TIMES = [
  { value: "ANY", label: "상관없음" },
  { value: "WITHIN_15", label: "15분 이내" },
  { value: "WITHIN_30", label: "30분 이내" },
  { value: "WITHIN_60", label: "1시간 이내" },
] as const satisfies readonly Option<string>[];

export const TOOLS = [
  { value: "FRYING_PAN", label: "프라이팬" },
  { value: "POT", label: "냄비" },
  { value: "MICROWAVE", label: "전자레인지" },
  { value: "AIR_FRYER", label: "에어프라이어" },
  { value: "OVEN", label: "오븐" },
  { value: "RICE_COOKER", label: "밥솥" },
] as const satisfies readonly Option<string>[];

export const METHODS = [
  { value: "STIR_FRY", label: "볶음" },
  { value: "GRILL", label: "구이" },
  { value: "STEAM", label: "찜" },
  { value: "BRAISE", label: "조림" },
  { value: "SOUP_STEW", label: "국 및 찌개" },
  { value: "SEASONED", label: "무침" },
  { value: "DEEP_FRY", label: "튀김" },
] as const satisfies readonly Option<string>[];

export const CUISINES = [
  { value: "KOREAN", label: "한식" },
  { value: "WESTERN", label: "양식" },
  { value: "CHINESE", label: "중식" },
  { value: "JAPANESE", label: "일식" },
  { value: "BUNSIK", label: "분식" },
  { value: "SIMPLE", label: "간편식" },
] as const satisfies readonly Option<string>[];

export type Taste = (typeof TASTES)[number]["value"];
export type CookTime = (typeof COOK_TIMES)[number]["value"];
export type Tool = (typeof TOOLS)[number]["value"];
export type Method = (typeof METHODS)[number]["value"];
export type Cuisine = (typeof CUISINES)[number]["value"];

/** 시트 하나에 하나만 고른다. 안 고르면 null(서버로 보내지 않음) */
export type CustomOptions = {
  taste: Taste | null;
  cookTime: CookTime | null;
  tool: Tool | null;
  method: Method | null;
  cuisine: Cuisine | null;
};

/** 커스텀 생성 요청(API 28 { mode: "CUSTOM", ... }). 고른 값의 ID·코드만 보낸다 */
export type CustomRecipeRequest = CustomOptions & {
  servings: number;
  ingredientIds: number[];
  allergenCodes: AllergenCode[];
};

/** 재료 선택 드로워 한 줄. 모양은 냉장고 목록(API 12) 응답을 따랐다 */
export type PickableIngredient = {
  id: number;
  name: string;
  categoryCode: CategoryCode;
  storage: StorageCode;
  amount: number;
  unit: FridgeUnit;
  expiryStatus: ExpiryStatus;
  /** 서버 문구 그대로. 예: "D-3", "D-DAY" */
  dDay: string;
};

/** 화면 처음 값: 가구원 수(API 5, 식사 인원 기본값·제안), 내 냉장고 재료(API 12) */
export type CustomRecipeDefaults = {
  householdSize: number;
  ingredients: PickableIngredient[];
};

/** 식사 인원 범위: 가구원 수와 같은 1~50 */
export const SERVINGS_MIN = 1;
export const SERVINGS_MAX = 50;

/** 미리보기용(피그마 p4.1.2-커스텀 레시피 생성-드로워 문구). 카테고리는 피그마 그림에 맞췄다 */
export const SAMPLE_CUSTOM_DEFAULTS: CustomRecipeDefaults = {
  householdSize: 1,
  ingredients: [
    {
      id: 1,
      name: "우유",
      categoryCode: "DAIRY_EGG",
      storage: "FRIDGE",
      amount: 500,
      unit: "ML",
      expiryStatus: "DANGER",
      dDay: "D-DAY",
    },
    {
      id: 2,
      name: "쪽파",
      categoryCode: "VEGETABLE",
      storage: "FRIDGE",
      amount: 1,
      unit: "BUNCH",
      expiryStatus: "SOON",
      dDay: "D-3",
    },
    {
      id: 3,
      name: "굴소스",
      categoryCode: "SEASONING",
      storage: "FRIDGE",
      amount: 350,
      unit: "ML",
      expiryStatus: "OK",
      dDay: "D-20",
    },
    {
      id: 4,
      name: "야채 믹스",
      categoryCode: "VEGETABLE",
      storage: "FRIDGE",
      amount: 600,
      unit: "G",
      expiryStatus: "OK",
      dDay: "D-35",
    },
    {
      id: 5,
      name: "당근",
      categoryCode: "VEGETABLE",
      storage: "FRIDGE",
      amount: 1,
      unit: "KG",
      expiryStatus: "OK",
      dDay: "D-49",
    },
    {
      id: 6,
      name: "마르까네 치즈",
      categoryCode: "DAIRY_EGG",
      storage: "FRIDGE",
      amount: 200,
      unit: "G",
      expiryStatus: "OK",
      dDay: "D-120",
    },
    {
      id: 7,
      name: "초고추장",
      categoryCode: "SEASONING",
      storage: "FRIDGE",
      amount: 500,
      unit: "ML",
      expiryStatus: "OK",
      dDay: "D-185",
    },
    {
      id: 8,
      name: "국간장",
      categoryCode: "SEASONING",
      storage: "FRIDGE",
      amount: 1,
      unit: "L",
      expiryStatus: "OK",
      dDay: "D-205",
    },
  ],
};
