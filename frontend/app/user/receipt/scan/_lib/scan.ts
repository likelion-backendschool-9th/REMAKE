// 영수증 스캔 결과(p3.3) 값. 모양은 API 20(POST /user/receipts/scan) 응답을 따랐다.
// OCR 결과는 저장하지 않는다. 등록(API 21) 전까지 화면 상태로만 들고 있고, 품목 수정(p3.3.1)·추가(p3.3.2)도 이 상태를 고친다.

import type { ExpiryStatus } from "@/components/ExpiryLabel/ExpiryLabel";
import type { TextSheetCopyByKind } from "@/components/IngredientTextSheet/IngredientTextSheet";
import { dayjs } from "@/lib/dayjs";
import type { IngredientDetail, IngredientDraft } from "@/lib/ingredient-form";
import type { CategoryCode, FridgeUnit, StorageCode } from "@/lib/ingredients";

export type ScanItem = {
  name: string;
  categoryCode: CategoryCode;
  storage: StorageCode;
  amount: number;
  unit: FridgeUnit;
  /** 원. 사용자가 지우면 null(재료 가격은 선택 항목) */
  price: number | null;
  /** 카테고리 기본 보관 기간으로 추정한 소비기한 "2026-11-26" */
  expirationDate: string;
};

export type ScanResult = {
  /** OCR이 못 읽으면 빈 값. 등록할 때는 둘 다 필요(domain-rules 3) */
  storeName: string | null;
  storeCode: string | null;
  purchasedOn: string | null;
  purchasedTime: string | null;
  receiptNumber: string | null;
  items: ScanItem[];
};

export const SCAN_PATH = "/user/receipt/scan";

export const SCAN_RESULT_PATH = `${SCAN_PATH}/result`;
export const SCAN_ITEM_NEW_PATH = `${SCAN_PATH}/items/new`;

/** 품목 수정(p3.3.1) 주소. OCR 품목은 id가 없어 순서로 찾는다 */
export function scanItemEditHref(index: number): string {
  return `${SCAN_PATH}/items/${index}/edit`;
}

/** 품목 가격 합(총 금액). 가격이 없는 품목은 0원 */
export function sumScanPrice(items: readonly ScanItem[]): number {
  return items.reduce((sum, item) => sum + (item.price ?? 0), 0);
}

/** OCR 품목 → 재료 입력 카드 값(냉장고 재료 수정과 같은 카드, lib/ingredient-form). id는 순서 */
export function scanItemToDetail(item: ScanItem, index: number): IngredientDetail {
  return { id: index, ...item };
}

/** 다 채운 입력 값 → OCR 품목. 필수 값이 비면 null(버튼이 꺼져 있어 보통 오지 않음) */
export function draftToScanItem(draft: IngredientDraft): ScanItem | null {
  const { name, categoryCode, storage, expirationDate, unit, amount, price } = draft;
  if (!name.trim() || !categoryCode || !storage || !expirationDate || !unit || amount <= 0)
    return null;
  return { name: name.trim(), categoryCode, storage, expirationDate, unit, amount, price };
}

/** 영수증 재료 수정·추가의 재료명·가격 시트 문구(피그마 p3.3.1·p3.3.2, 냉장고 시트와 다름) */
export const RECEIPT_TEXT_SHEET_COPY: TextSheetCopyByKind = {
  name: {
    title: "재료명 수정",
    placeholder: "변경할 재료명을 작성해 주세요.",
    confirmLabel: "수정",
    notes: ["제품명이 아닌 재료명을 작성해 주세요.", "비비고 냉동 만두 X → 만두 O"],
  },
  price: {
    title: "재료 가격",
    placeholder: "변경할 재료 가격을 작성해 주세요",
    confirmLabel: "수정",
    notes: ["가격을 정확히 작성할수록 소비 패턴 분석률이 올라가요"],
  },
};

/** OCR 결과 ⋮ 시트(피그마 p3.3-ocr 결과-케밥 선택). 맨 위 항목이 기본 선택(blueprint §6) */
export const RESULT_MENU = [
  { value: "ADD", label: "재료 추가" },
  { value: "RESCAN", label: "영수증 재촬영" },
] as const;

export type ResultMenuAction = (typeof RESULT_MENU)[number]["value"];

export const EXPIRY_NOTICE =
  "소비 기한은 카테고리 기본 보관 기간으로 추정했어요. 소비기한을 확인하시고 수정해 주세요";

/**
 * 소비기한 날짜 → 상태와 문구(D-N, D-DAY, 폐기-N). 기준은 domain-rules 2(지남·당일=위험, 1~3일=임박, 그 외=여유).
 * 냉장고(저장된 재료)는 서버가 준 status·dDay를 그대로 쓴다. 등록 전 OCR 결과는 저장 전이고 사용자가 소비기한을 고치므로
 * 이 화면에서만 계산한다(11/25 결정, blueprint §10 예외). 오늘은 서버와 같게 KST 날짜(lib/dayjs todayInKst)를 넘긴다.
 */
export function getExpiry(
  expirationDate: string,
  today: string,
): { status: ExpiryStatus; text: string } {
  const days = dayjs(expirationDate).diff(dayjs(today), "day");
  if (days < 0) return { status: "DANGER", text: `폐기-${-days}` };
  if (days === 0) return { status: "DANGER", text: "D-DAY" };
  return { status: days <= 3 ? "SOON" : "OK", text: `D-${days}` };
}

/** 마크업 확인용 값(피그마 p3.3-ocr 결과와 같은 품목). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_SCAN_RESULT: ScanResult = {
  storeName: "GS 프레쉬 마트 봉천점",
  storeCode: "GS_FRESH",
  purchasedOn: "2026-11-21",
  purchasedTime: "14:32",
  receiptNumber: "0412-0087",
  items: [
    {
      name: "오겹살",
      categoryCode: "MEAT",
      storage: "FREEZER",
      amount: 600,
      unit: "G",
      price: 23400,
      expirationDate: "2026-11-26",
    },
    {
      name: "돼지 목살",
      categoryCode: "MEAT",
      storage: "FREEZER",
      amount: 200,
      unit: "G",
      price: 11500,
      expirationDate: "2026-11-28",
    },
  ],
};

/** 품목 수정 확인용 값(피그마 p3.3.1 위 요약 줄 "목살 · 냉동실 · 500g · 2026년 12월 21일"). 미리보기(app/dev)에서만 쓴다 */
export const SAMPLE_SCAN_EDIT_ITEM: ScanItem = {
  name: "목살",
  categoryCode: "MEAT",
  storage: "FREEZER",
  amount: 500,
  unit: "G",
  price: 14500,
  expirationDate: "2026-12-21",
};
