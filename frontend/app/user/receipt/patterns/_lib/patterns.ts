// 나의 소비(p3.4)·버린 식재료(p3.4.1) 값. 모양은 API 25(GET /user/expenses/patterns)·26(GET /user/expenses/daily-items)을 따랐다.
// 버린 식비(API 25 discardedAmount)와 버린 식재료 목록(API 61)은 #248 제안으로 api-summary에 올라 있다(확정 전).
// TODO: API를 붙이면 SAMPLE_* 대신 서버 값을 쓴다.

import { dayjs } from "@/lib/dayjs";
import { formatNumber } from "@/lib/format";
import type { CategoryCode, FridgeUnit, StorageCode } from "@/lib/ingredients";

export const PATTERNS_PATH = "/user/receipt/patterns";
export const DISCARDED_PATH = `${PATTERNS_PATH}/discarded`;

/** 소비 패턴을 볼 수 있는 달 수(영수증 6개월 보관, domain-rules 3) */
export const PATTERN_MONTHS = 6;

export type CategorySpending = { categoryCode: CategoryCode; amount: number; ratio: number };

export type SpendingPattern = {
  /** "2026-11" */
  month: string;
  totalAmount: number;
  /** 내 평균과의 차이(원). 양수=더, 음수=덜, null=비교할 기록 없음(숨김) */
  averageDiffAmount: number | null;
  /** 그 달 버린 식비(원). #248 제안 */
  discardedAmount: number;
  categories: CategorySpending[];
  /** 소비가 있는 날만 */
  dailyTotals: { date: string; amount: number }[];
};

export type DailyItem = {
  name: string;
  categoryCode: CategoryCode;
  imageUrl: string | null;
  storage: StorageCode;
  amount: number;
  unit: FridgeUnit;
  price: number;
};

export type DailyItems = { date: string; totalAmount: number; items: DailyItem[] };

export type DiscardedItem = {
  name: string;
  categoryCode: CategoryCode;
  /** 버린 날 "2026-11-20" */
  discardedOn: string;
  /** 버린 금액(가격 × 버린 양 ÷ 처음 양). 가격이 없으면 null(domain-rules 2) */
  amount: number | null;
};

export type DiscardedMonth = { month: string; totalAmount: number; items: DiscardedItem[] };

/**
 * 막대그래프·범례 색과 짧은 이름(피그마 p3.4 범례 순서). 피그마 값이고 토큰은 없다.
 * 범례 이름은 피그마처럼 짧게 쓴다(lib/ingredients의 이름과 다름).
 */
export const CATEGORY_LEGEND: { code: CategoryCode; label: string; color: string }[] = [
  { code: "VEGETABLE", label: "채소", color: "#2e9e5d" },
  { code: "FRUIT", label: "과일", color: "#ce3b78" },
  { code: "MEAT", label: "육류", color: "#c24747" },
  { code: "SEAFOOD", label: "해산물", color: "#358ad4" },
  { code: "DAIRY_EGG", label: "달걀·유제품", color: "#a58812" },
  { code: "GRAIN", label: "곡류·면", color: "#b98046" },
  { code: "BEAN_TOFU", label: "콩·두부", color: "#779933" },
  { code: "PROCESSED", label: "냉동식품", color: "#9947c2" },
  { code: "SIDE_DISH", label: "김치·반찬", color: "#da5d2f" },
  { code: "SEASONING", label: "양념", color: "#2e9e8b" },
  { code: "BEVERAGE", label: "음료", color: "#4641c8" },
  { code: "ETC", label: "기타", color: "#7b838e" },
];

export function getLegend(code: CategoryCode) {
  return (
    CATEGORY_LEGEND.find((item) => item.code === code) ??
    CATEGORY_LEGEND[CATEGORY_LEGEND.length - 1]
  );
}

/** 오늘부터 지난 6개월 "2026-06" … "2026-11" (오래된 달 먼저, 탭 왼쪽부터) */
export function patternMonths(today: string): string[] {
  const current = dayjs(today).startOf("month");
  return Array.from({ length: PATTERN_MONTHS }, (_, i) =>
    current.subtract(PATTERN_MONTHS - 1 - i, "month").format("YYYY-MM"),
  );
}

/** 고른 날이 든 주의 월~일 7일 */
export function weekOf(date: string): string[] {
  const day = dayjs(date);
  // 월요일 시작: 일요일(0)은 6일 전이 월요일
  const monday = day.subtract((day.day() + 6) % 7, "day");
  return Array.from({ length: 7 }, (_, i) => monday.add(i, "day").format("YYYY-MM-DD"));
}

/** 그 달 1일이 든 주부터 말일이 든 주까지(좌우로 넘기는 날짜 줄, domain-rules 3) */
export function monthWeeks(month: string): string[][] {
  const last = dayjs(`${month}-01`).endOf("month").format("YYYY-MM-DD");
  const weeks = [weekOf(`${month}-01`)];
  while (!weeks[weeks.length - 1].includes(last)) {
    const nextMonday = dayjs(weeks[weeks.length - 1][6])
      .add(1, "day")
      .format("YYYY-MM-DD");
    weeks.push(weekOf(nextMonday));
  }
  return weeks;
}

export const WEEKDAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"] as const;

/**
 * 처음 고를 날: 이번 달이면 오늘, 지난 달이면 마지막으로 산 날(없으면 1일). domain-rules 3(제안)
 */
export function defaultDate(month: string, today: string, pattern?: SpendingPattern): string {
  if (today.startsWith(month)) return today;
  const last = pattern?.dailyTotals.at(-1)?.date;
  return last ?? `${month}-01`;
}

/**
 * 평균 차이 문구(피그마 "평균보다 8만원 더 지출"). 1만원 이상은 만원 단위로 반올림, 그 밑은 원 단위(11/25 확정).
 * 0이면 "평균과 같게 지출"(제안)
 */
export function formatPatternAverageDiff(diff: number): string {
  if (diff === 0) return "평균과 같게 지출";
  const abs = Math.abs(diff);
  const amount =
    abs >= 10000 ? `${formatNumber(Math.round(abs / 10000))}만원` : `${formatNumber(abs)}원`;
  return `평균보다 ${amount} ${diff > 0 ? "더" : "덜"} 지출`;
}

/** 마크업 확인용 값(피그마 p3.4-소비 패턴과 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_PATTERN: SpendingPattern = {
  month: "2026-11",
  totalAmount: 403771,
  averageDiffAmount: 80000,
  discardedAmount: 4700,
  categories: [
    { categoryCode: "PROCESSED", amount: 125000, ratio: 0.31 },
    { categoryCode: "MEAT", amount: 56000, ratio: 0.14 },
    { categoryCode: "VEGETABLE", amount: 133000, ratio: 0.33 },
    { categoryCode: "GRAIN", amount: 68000, ratio: 0.17 },
    { categoryCode: "ETC", amount: 21771, ratio: 0.05 },
  ],
  dailyTotals: [
    { date: "2026-11-04", amount: 98500 },
    { date: "2026-11-12", amount: 184871 },
    { date: "2026-11-20", amount: 120400 },
  ],
};

export const SAMPLE_DAILY_ITEMS: DailyItems = {
  date: "2026-11-20",
  totalAmount: 120400,
  items: [
    {
      name: "목살",
      categoryCode: "MEAT",
      imageUrl: null,
      storage: "FREEZER",
      amount: 600,
      unit: "G",
      price: 19500,
    },
    {
      name: "오렌지 주스",
      categoryCode: "BEVERAGE",
      imageUrl: null,
      storage: "FRIDGE",
      amount: 1.5,
      unit: "L",
      price: 7500,
    },
    {
      name: "달걀",
      categoryCode: "DAIRY_EGG",
      imageUrl: null,
      storage: "FRIDGE",
      amount: 30,
      unit: "EA",
      price: 6500,
    },
    {
      name: "키조개 관자",
      categoryCode: "SEAFOOD",
      imageUrl: null,
      storage: "FREEZER",
      amount: 1,
      unit: "KG",
      price: 30500,
    },
    {
      name: "칵테일 새우",
      categoryCode: "SEAFOOD",
      imageUrl: null,
      storage: "FREEZER",
      amount: 1,
      unit: "KG",
      price: 20500,
    },
    {
      name: "배추",
      categoryCode: "VEGETABLE",
      imageUrl: null,
      storage: "FREEZER",
      amount: 1,
      unit: "KG",
      price: 7800,
    },
  ],
};

/** 고른 날에 소비가 없는 상태(피그마 p3.4-소비 패턴-빈 리스트: 11월 17일) */
export const SAMPLE_EMPTY_DAY: DailyItems = { date: "2026-11-17", totalAmount: 0, items: [] };

/** 마크업 확인용 값(피그마 p3.4.1-이번 달 버린 식재료와 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_DISCARDED: DiscardedMonth = {
  month: "2026-11",
  totalAmount: 4700,
  items: [
    { name: "대파", categoryCode: "VEGETABLE", discardedOn: "2026-11-20", amount: 2250 },
    { name: "우유", categoryCode: "DAIRY_EGG", discardedOn: "2026-11-18", amount: 1450 },
    { name: "양파", categoryCode: "VEGETABLE", discardedOn: "2026-11-12", amount: 1000 },
    { name: "두부", categoryCode: "BEAN_TOFU", discardedOn: "2026-11-10", amount: null },
  ],
};
