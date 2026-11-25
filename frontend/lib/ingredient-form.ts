// 재료 입력 값. 냉장고 재료 추가(p2.2)·수정(p2.1)과 영수증 재료 추가(p3.3.2)·수정(p3.3.1)이 같이 쓴다.
// 모양은 API 13(재료 하나)·15(추가)·16(수정)에서 화면이 쓰는 것만 따랐다.
// 가격만 선택이고 나머지는 모두 채워야 버튼이 켜진다(domain-rules 2).

import {
  type CategoryCode,
  formatAmount,
  FRIDGE_UNITS,
  type FridgeUnit,
  getStorage,
  type StorageCode,
} from "@/lib/ingredients";

/** 입력 상한: 백엔드 요청 검증(IngredientCreateRequest·IngredientUpdateRequest)과 맞춘다 */
/** 재료명 최대 글자 수(@Size(max = 50)) */
export const INGREDIENT_NAME_MAX = 50;
/** 가격 최대 자릿수. 서버는 Long이지만, JS 숫자로 바꿔도 값이 바뀌지 않는 범위(15자리)로 막는다 */
export const PRICE_MAX_DIGITS = 15;
/** 수량 최대값: 정수 7자리·소수 3자리(1천만 미만) */
export const AMOUNT_MAX = 9_999_999;

export type IngredientDraft = {
  name: string;
  categoryCode: CategoryCode | null;
  storage: StorageCode | null;
  /** "2026-12-21" */
  expirationDate: string | null;
  /** 원. 없으면 null(선택 항목) */
  price: number | null;
  amount: number;
  /** 서버가 재료명으로 고른 단위. 재료명을 쓰기 전에는 null이고 수량 스테퍼가 꺼진다 */
  unit: FridgeUnit | null;
};

/** API 13 응답 중 수정 화면이 쓰는 값 */
export type IngredientDetail = {
  id: number;
  name: string;
  categoryCode: CategoryCode;
  storage: StorageCode;
  expirationDate: string;
  price: number | null;
  amount: number;
  unit: FridgeUnit;
};

/**
 * 재료 추가의 처음 값. 보관 위치는 기본 옵션 냉장을 미리 골라 둔다(blueprint §6).
 * 카테고리는 잘못 미리 고르면 그대로 저장되는 값이라 비워 두고, 고르기 전에는 저장 버튼이 꺼진다(isDraftComplete).
 * 재료명·소비 기한도 사용자가 정해야 해서 비워 둔다
 */
export const EMPTY_DRAFT: IngredientDraft = {
  name: "",
  categoryCode: null,
  storage: "FRIDGE",
  expirationDate: null,
  price: null,
  amount: 0,
  unit: null,
};

export function toDraft(detail: IngredientDetail): IngredientDraft {
  const { name, categoryCode, storage, expirationDate, price, amount, unit } = detail;
  return { name, categoryCode, storage, expirationDate, price, amount, unit };
}

/** 가격을 뺀 모든 값이 있고 수량이 0보다 크면 저장할 수 있다 */
export function isDraftComplete(draft: IngredientDraft): boolean {
  return Boolean(
    draft.name.trim() &&
    draft.categoryCode &&
    draft.storage &&
    draft.expirationDate &&
    draft.unit &&
    draft.amount > 0,
  );
}

/** 수정 화면: 바뀐 값이 있는지(API 16은 바뀐 값만 보낸다) */
export function isDraftChanged(draft: IngredientDraft, saved: IngredientDetail): boolean {
  const original = toDraft(saved);
  return (Object.keys(original) as (keyof IngredientDraft)[]).some(
    (key) => draft[key] !== original[key],
  );
}

/**
 * 단위 고르기. TODO: API 14(GET /user/ingredient-units?name=&categoryCode=)로 바꾼다.
 * 지금은 마크업 확인용으로 몇 개만 정해 두고 나머지는 서버 기본값처럼 EA로 둔다.
 */
const SAMPLE_UNITS: Record<string, FridgeUnit> = {
  목살: "G",
  우둔살: "G",
  삼겹살: "G",
  우유: "ML",
  굴소스: "ML",
  쪽파: "BUNCH",
  대파: "BUNCH",
  마늘: "PIECE",
  김: "SHEET",
  두부: "PACK",
};

export function pickSampleUnit(name: string): FridgeUnit {
  return SAMPLE_UNITS[name.trim()] ?? "EA";
}

/** 처음 단위가 정해질 때 수량: 스테퍼 한 칸(50g, 1개 ...) */
export function firstAmount(unit: FridgeUnit): number {
  return FRIDGE_UNITS[unit].step;
}

function splitDate(date: string) {
  const [year, month, day] = date.split("-").map(Number);
  return { year, month, day };
}

/** "2026-12-21" → 설정 행 "26년 12월 21일" / 요약 줄 "2026년 12월 21일"(피그마 p2.1) */
export function formatExpiry(date: string, style: "short" | "long" = "short"): string {
  const { year, month, day } = splitDate(date);
  const shownYear = style === "short" ? String(year).slice(-2) : year;
  return `${shownYear}년 ${month}월 ${day}일`;
}

/** 14500 → "14,500원" */
export function formatPrice(price: number): string {
  return `${price.toLocaleString("ko-KR")}원`;
}

/** 수정 화면 위 요약: "냉동실 · 500g · 2026년 12월 21일" */
export function describeIngredient(detail: IngredientDetail): string {
  return [
    getStorage(detail.storage).place,
    formatAmount(detail.amount, detail.unit),
    formatExpiry(detail.expirationDate, "long"),
  ].join(" · ");
}
