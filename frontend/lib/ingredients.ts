// 재료 공통 값: 카테고리 12개(ERD 부록 A 순서), 보관 위치 3개, 냉장고 단위 9개(domain-rules "Units").
// 냉장고·재료 추가·수정에서 쓰고, 이후 영수증·장바구니도 같은 값을 쓴다.

export const CATEGORIES = [
  { code: "VEGETABLE", name: "채소", image: "vegetable" },
  { code: "MEAT", name: "육류", image: "meat" },
  { code: "FRUIT", name: "과일", image: "fruit" },
  { code: "SEAFOOD", name: "해산물", image: "seafood" },
  { code: "DAIRY_EGG", name: "달걀·유제품", image: "dairy-egg" },
  { code: "GRAIN", name: "곡류·면·빵", image: "grain" },
  { code: "BEAN_TOFU", name: "두부·콩류", image: "bean-tofu" },
  { code: "PROCESSED", name: "가공·냉동식품", image: "processed" },
  { code: "SIDE_DISH", name: "김치·반찬", image: "side-dish" },
  { code: "SEASONING", name: "양념", image: "seasoning" },
  { code: "BEVERAGE", name: "음료", image: "beverage" },
  { code: "ETC", name: "기타", image: "etc" },
] as const;

export type CategoryCode = (typeof CATEGORIES)[number]["code"];

export function getCategory(code: CategoryCode) {
  return CATEGORIES.find((category) => category.code === code) ?? CATEGORIES[CATEGORIES.length - 1];
}

/** 카테고리 기본 이미지(재료 행 왼쪽 그림, 3:2) */
export function getCategoryImage(code: CategoryCode): string {
  return `/images/categories/${getCategory(code).image}.webp`;
}

/** 보관 위치. label=선택 시트·설정 값, place=재료 행 부제("냉동실 · 600g") */
export const STORAGES = [
  { code: "FRIDGE", label: "냉장", place: "냉장실" },
  { code: "FREEZER", label: "냉동", place: "냉동실" },
  { code: "PANTRY", label: "실온", place: "실온" },
] as const;

export type StorageCode = (typeof STORAGES)[number]["code"];

export function getStorage(code: StorageCode) {
  return STORAGES.find((storage) => storage.code === code) ?? STORAGES[0];
}

/** 냉장고 단위 9개. step=스테퍼 한 번에 바뀌는 양(blueprint §10: g·ml 50, kg·L 0.5, 나머지 1) */
export const FRIDGE_UNITS = {
  G: { label: "g", step: 50 },
  KG: { label: "kg", step: 0.5 },
  ML: { label: "ml", step: 50 },
  L: { label: "L", step: 0.5 },
  EA: { label: "개", step: 1 },
  PACK: { label: "팩", step: 1 },
  BUNCH: { label: "단", step: 1 },
  PIECE: { label: "쪽", step: 1 },
  SHEET: { label: "장", step: 1 },
} as const;

export type FridgeUnit = keyof typeof FRIDGE_UNITS;

/** 600, "G" → "600g" / 1, "BUNCH" → "1단" */
export function formatAmount(amount: number, unit: FridgeUnit): string {
  return `${amount}${FRIDGE_UNITS[unit].label}`;
}

/** 레시피·장바구니 단위 14개 이름(domain-rules "Units"). 냉장고 단위 9개 + 계량 단위 5개 */
export const UNIT_LABELS = {
  ...Object.fromEntries(Object.entries(FRIDGE_UNITS).map(([code, { label }]) => [code, label])),
  TBSP: "큰술",
  TSP: "작은술",
  CUP: "컵",
  PINCH: "꼬집",
  TO_TASTE: "약간",
} as Record<FridgeUnit | "TBSP" | "TSP" | "CUP" | "PINCH" | "TO_TASTE", string>;

export type RecipeUnit = keyof typeof UNIT_LABELS;

/** 2, "TBSP" → "2큰술" / 양이 없거나 TO_TASTE면 "약간"(domain-rules 표기) */
export function formatUnitAmount(amount: number | null, unit: RecipeUnit): string {
  if (amount === null || unit === "TO_TASTE") return UNIT_LABELS.TO_TASTE;
  return `${amount}${UNIT_LABELS[unit]}`;
}
