// 식단표(p5) 값. 모양은 API 39(달력)·40(하루 식단) 응답에서 화면이 쓰는 것만 따랐다.
// TODO: API 39·40을 붙이면 서버 값을 쓴다. 남은 생성 횟수는 API 27(usage-quotas)의 meal plan.

import { dayjs } from "@/lib/dayjs";

import type { MealType } from "../../_lib/meal-plan";

export type MealPlanPeriod = { mealPlanId: number; startDate: string; endDate: string };

export type MealPlanCalendar = {
  /** "2026-11" */
  month: string;
  periods: MealPlanPeriod[];
  datesWithMeals: string[];
};

export type MealSlot = {
  slotId: number;
  mealType: MealType;
  recipeId: number;
  title: string;
  /** 재료 이름 몇 개(서버가 고름) */
  ingredientSummary: string[];
  /** 요약에 안 보인 재료 수(+N) */
  extraIngredientCount: number;
  favorite: boolean;
  cooked: boolean;
};

export type CalendarDay = {
  /** "2026-11-17" */
  date: string;
  day: number;
  isOtherMonth: boolean;
  /** 식단이 있는 날(파란 띠) */
  hasMeals: boolean;
  /** 식단 있는 날이 이어진 묶음의 첫날·끝날(띠 끝을 둥글게) */
  isRunStart: boolean;
  isRunEnd: boolean;
  /** 식단 기간 시작일(파란 글자) */
  isPeriodStart: boolean;
};

/**
 * 달력 칸: 그 달 1일이 있는 주의 일요일부터, 마지막 날이 있는 주의 토요일까지.
 * 파란 띠는 식단 기간 전체가 아니라 식단이 있는 날(datesWithMeals)에만 칠한다. 기간 안이라도 식단을 지웠거나
 * 재료가 부족해 만들지 못한 날은 비워 둔다.
 */
export function buildMonthGrid(
  month: string,
  periods: readonly MealPlanPeriod[],
  datesWithMeals: readonly string[],
): CalendarDay[][] {
  const mealDates = new Set(datesWithMeals);
  const first = dayjs(`${month}-01`);
  const start = first.subtract(first.day(), "day");
  const last = first.endOf("month");
  const end = last.add(6 - last.day(), "day");
  const weeks: CalendarDay[][] = [];
  for (let cursor = start; !cursor.isAfter(end, "day"); cursor = cursor.add(1, "day")) {
    if (cursor.day() === 0) weeks.push([]);
    const date = cursor.format("YYYY-MM-DD");
    const hasMeals = mealDates.has(date);
    weeks.at(-1)?.push({
      date,
      day: cursor.date(),
      isOtherMonth: cursor.month() !== first.month(),
      hasMeals,
      isRunStart: hasMeals && !mealDates.has(cursor.subtract(1, "day").format("YYYY-MM-DD")),
      isRunEnd: hasMeals && !mealDates.has(cursor.add(1, "day").format("YYYY-MM-DD")),
      isPeriodStart: periods.some((p) => p.startDate === date),
    });
  }
  return weeks;
}

/** "2026-11" → "2026년 11월" */
export function formatMonthTitle(month: string): string {
  const [year, monthNumber] = month.split("-").map(Number);
  return `${year}년 ${monthNumber}월`;
}

/** "2026-11-17" → "11월 17일 식단" */
export function formatDayTitle(date: string): string {
  const value = dayjs(date);
  return `${value.month() + 1}월 ${value.date()}일 식단`;
}

/** 마크업 확인용 값(피그마 p5-식단표와 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_CALENDAR: MealPlanCalendar = {
  month: "2026-11",
  periods: [{ mealPlanId: 12, startDate: "2026-11-16", endDate: "2026-11-23" }],
  datesWithMeals: [
    "2026-11-16",
    "2026-11-17",
    "2026-11-18",
    "2026-11-19",
    "2026-11-20",
    "2026-11-21",
    "2026-11-22",
    "2026-11-23",
  ],
};

export const SAMPLE_SLOTS: MealSlot[] = [
  {
    slotId: 101,
    mealType: "LUNCH",
    recipeId: 31,
    title: "참치 김치찌개",
    ingredientSummary: ["김치", "다시다 조미료", "참치캔"],
    extraIngredientCount: 2,
    favorite: false,
    cooked: false,
  },
  {
    slotId: 102,
    mealType: "DINNER",
    recipeId: 32,
    title: "떡만두 미역국",
    ingredientSummary: ["미역", "냉동 만두", "계란", "떡"],
    extraIngredientCount: 1,
    favorite: true,
    cooked: false,
  },
];
