// 식단표(p5)와 식단표 생성(p5.1)이 같이 쓰는 값(domain-rules 5).

export const MEAL_TYPES = [
  { code: "BREAKFAST", label: "아침" },
  { code: "LUNCH", label: "점심" },
  { code: "DINNER", label: "저녁" },
] as const;

export type MealType = (typeof MEAL_TYPES)[number]["code"];

export function getMealLabel(code: MealType): string {
  return MEAL_TYPES.find((meal) => meal.code === code)?.label ?? code;
}

/** 식단 목적 5개(피그마 p5.1 식단 목적 시트 순서). 기본은 균형(BALANCED), 마지막에 고른 목적을 다음 기본으로 기억한다 */
export const MEAL_PURPOSES = [
  { code: "BALANCED", label: "균형 잡힌 식단" },
  { code: "HIGH_PROTEIN", label: "고단백 식단" },
  { code: "DIET", label: "다이어트 식단" },
  { code: "SAVING", label: "재료 절약 식단" },
  { code: "SIMPLE", label: "간편 식단" },
] as const;

export type MealPurpose = (typeof MEAL_PURPOSES)[number]["code"];

export const MEAL_PLAN_SETUP_PATH = "/user/meal-plan/meal-plans/new";

/** 생성 화면·추가 시트 아래 안내(domain-rules 5) */
export const MEAL_PLAN_FAIL_NOTICE = "식단표 생성에 실패하면 횟수는 차감하지 않아요";
