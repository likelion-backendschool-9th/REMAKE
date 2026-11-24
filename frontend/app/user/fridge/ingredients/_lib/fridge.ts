// 냉장고 화면 값. 모양은 냉장고 목록 API(domain-rules 2) 응답에서 화면이 쓰는 것만 따랐다.
// 소비기한 상태(expiryStatus)와 문구(dDay)는 서버가 준 값을 그대로 보인다(기기 시계로 다시 계산하지 않음).
// TODO: 냉장고 목록 API를 붙이면 SAMPLE_INGREDIENTS 대신 서버 값을 쓴다.

import type { ExpiryStatus } from "@/components/ExpiryLabel/ExpiryLabel";
import type { CategoryCode, FridgeUnit, StorageCode } from "@/lib/ingredients";

export type FridgeIngredient = {
  id: number;
  name: string;
  categoryCode: CategoryCode;
  storage: StorageCode;
  amount: number;
  unit: FridgeUnit;
  expiryStatus: ExpiryStatus;
  /** 서버 문구 그대로. 예: "D-3", "D-DAY", "폐기-1" */
  dDay: string;
};

/** 상태별 이름(목록 제목 "N개의 위험 재료", 요약 카드) */
export const STATUS_LABEL: Record<ExpiryStatus, string> = {
  DANGER: "위험",
  SOON: "임박",
  OK: "여유",
};

export const STATUS_ORDER: readonly ExpiryStatus[] = ["DANGER", "SOON", "OK"];

/** 마크업 확인용 값(피그마 p2-냉장고와 같음). 서버가 소비기한이 가까운 순으로 준다 */
export const SAMPLE_INGREDIENTS: FridgeIngredient[] = [
  {
    id: 1,
    name: "목살",
    categoryCode: "MEAT",
    storage: "FREEZER",
    amount: 600,
    unit: "G",
    expiryStatus: "DANGER",
    dDay: "폐기-1",
  },
  {
    id: 2,
    name: "우유",
    categoryCode: "DAIRY_EGG",
    storage: "FRIDGE",
    amount: 500,
    unit: "ML",
    expiryStatus: "DANGER",
    dDay: "D-DAY",
  },
  {
    id: 3,
    name: "쪽파",
    categoryCode: "VEGETABLE",
    storage: "FRIDGE",
    amount: 1,
    unit: "BUNCH",
    expiryStatus: "SOON",
    dDay: "D-3",
  },
  {
    id: 4,
    name: "굴소스",
    categoryCode: "SEASONING",
    storage: "FRIDGE",
    amount: 350,
    unit: "ML",
    expiryStatus: "OK",
    dDay: "D-365",
  },
];

/** 알림 띠 예시(서버가 주는 가장 급한 알림 한 줄) */
export const SAMPLE_NOTICE = { message: "당근의 소비기한이 임박했어요!", unreadCount: 2 };
