// 영수증 목록(p3) 값. 모양은 API 22(GET /user/receipts?month=) 응답을 따랐다.
// TODO: 영수증 목록 API를 붙이면 서버 값을 쓴다. 월 합계·평균 차이·주차 묶음은 서버가 계산해 준다.

import { dayjs } from "@/lib/dayjs";
import { formatNumber } from "@/lib/format";
import type { CategoryCode, FridgeUnit, StorageCode } from "@/lib/ingredients";

export type ReceiptRow = {
  id: number;
  /** 매장 코드(StoreBadge). 모르면 null */
  storeCode: string | null;
  storeName: string;
  /** "2026-11-18" */
  purchasedOn: string;
  itemCount: number;
  totalAmount: number;
};

export type ReceiptWeek = {
  /** 서버 문구. 예: "11월 4주차" */
  label: string;
  startDate: string;
  endDate: string;
  totalAmount: number;
  receipts: ReceiptRow[];
};

export type ReceiptMonth = {
  /** "2026-11" */
  month: string;
  totalAmount: number;
  /** 내 평균과의 차이(원). 양수=더 씀, 음수=덜 씀, null=비교할 기록 없음(숨김, 제안) */
  averageDiffAmount: number | null;
  weeks: ReceiptWeek[];
};

/** 영수증을 볼 수 있는 달 수(구매일 6개월 보관, domain-rules 3) */
export const RECEIPT_MONTHS = 6;

/** "2026-11-18" → "11월 18일(수)" */
export function formatPurchaseDay(date: string): string {
  return dayjs(date).format("M월 D일(ddd)");
}

/** 오늘부터 지난 6개월 "2026-11", "2026-10" … (최신 먼저) */
export function recentMonths(today: string): string[] {
  return Array.from({ length: RECEIPT_MONTHS }, (_, i) =>
    dayjs(today).startOf("month").subtract(i, "month").format("YYYY-MM"),
  );
}

/** 평균 차이 문구. 0이면 "내 평균과 같게 썼어요"(피그마 없음, 제안) */
export function formatAverageDiff(diff: number): string {
  if (diff === 0) return "내 평균과 같게 썼어요";
  const amount = formatNumber(Math.abs(diff));
  return diff > 0 ? `내 평균보다 ${amount}원 더 썼어요` : `내 평균보다 ${amount}원 덜 썼어요`;
}

/** 영수증 상세(API 23) */
export type ReceiptDetail = {
  id: number;
  storeCode: string | null;
  storeName: string;
  /** "2026-11-21" */
  purchasedOn: string;
  /**
   * 구매 시각 "14:32". 피그마 p3.2는 시각까지 보이지만 API 23 응답에는 없다(확인 필요).
   * 없으면 날짜만 보인다
   */
  purchasedTime?: string | null;
  /** 영수증 원본 이미지. 없으면 원본 칸을 그리지 않는다 */
  imageUrl: string | null;
  items: ReceiptItem[];
  totalAmount: number;
};

export type ReceiptItem = {
  name: string;
  categoryCode: CategoryCode;
  /** 재료 그림. 없으면 카테고리 기본 그림 */
  imageUrl: string | null;
  storage: StorageCode;
  amount: number;
  unit: FridgeUnit;
  price: number;
};

/** "2026-11-21", "14:32" → "2026. 11. 21.(토) 14:32" */
export function formatPurchaseDateTime(date: string, time?: string | null): string {
  const day = dayjs(date).format("YYYY. M. D.(ddd)");
  return time ? `${day} ${time}` : day;
}

/** 마크업 확인용 값(피그마 p3-영수증과 같음 + 지난주 묶음 하나). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_RECEIPT_MONTH: ReceiptMonth = {
  month: "2026-11",
  totalAmount: 214500,
  averageDiffAmount: 25000,
  weeks: [
    {
      label: "11월 4주차",
      startDate: "2026-11-22",
      endDate: "2026-11-29",
      totalAmount: 149500,
      receipts: [
        {
          id: 31,
          storeCode: "CU",
          storeName: "CU 편의점 연신내점",
          purchasedOn: "2026-11-18",
          itemCount: 12,
          totalAmount: 98500,
        },
        {
          id: 30,
          storeCode: "GS25",
          storeName: "GS25 편의점 연신내점",
          purchasedOn: "2026-11-16",
          itemCount: 4,
          totalAmount: 52000,
        },
      ],
    },
    {
      label: "11월 3주차",
      startDate: "2026-11-15",
      endDate: "2026-11-21",
      totalAmount: 65000,
      receipts: [
        {
          id: 29,
          storeCode: "EMART",
          storeName: "이마트 은평점",
          purchasedOn: "2026-11-15",
          itemCount: 7,
          totalAmount: 65000,
        },
      ],
    },
  ],
};

/** 마크업 확인용 값(피그마 p3.2-영수증 디테일과 같음). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_RECEIPT_DETAIL: ReceiptDetail = {
  id: 28,
  storeCode: "GS_FRESH",
  storeName: "GS 프레쉬 마트 봉천점",
  purchasedOn: "2026-11-21",
  purchasedTime: "14:32",
  imageUrl: "/images/receipt/sample-receipt.webp",
  totalAmount: 95800,
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
    {
      name: "당근",
      categoryCode: "VEGETABLE",
      imageUrl: null,
      storage: "FREEZER",
      amount: 1,
      unit: "KG",
      price: 3500,
    },
  ],
};
