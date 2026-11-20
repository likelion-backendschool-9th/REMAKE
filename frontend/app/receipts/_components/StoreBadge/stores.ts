// 영수증 판매처(마트) 배지 데이터. code는 백엔드 store_code와 같다.
// 판매처 문자열 매칭(include/exclude)은 백엔드가 하므로 여기에는 두지 않는다.
// bg/fg는 StoreBadge.module.css의 data-store 색과 같은 값으로 유지한다(확인용 화면에서 비교).
// label은 피그마 "브랜드 로고" 프레임(node 5687:2589)의 약어. ETC는 피그마에 없어 임시 약어.

export const STORES = {
  CU: { name: "CU", label: "CU", bg: "#652F8D", fg: "#FFFFFF" },
  GS25: { name: "GS25", label: "GS25", bg: "#0A9BD9", fg: "#FFFFFF" },
  SEVEN_ELEVEN: { name: "세븐일레븐", label: "7", bg: "#F58220", fg: "#FFFFFF" },
  EMART24: { name: "이마트24", label: "e24", bg: "#FFB81C", fg: "#231F20" },
  EMART: { name: "이마트", label: "E", bg: "#FFD400", fg: "#231F20" },
  HOMEPLUS: { name: "홈플러스", label: "H+", bg: "#E31837", fg: "#FFFFFF" },
  LOTTE_MART: { name: "롯데마트", label: "L", bg: "#ED1C24", fg: "#FFFFFF" },
  COSTCO: { name: "코스트코", label: "C", bg: "#005DAA", fg: "#FFFFFF" },
  TRADERS: { name: "이마트 트레이더스", label: "T", bg: "#1A1A1A", fg: "#FFD400" },
  HANARO: { name: "농협 하나로마트", label: "농협", bg: "#009A44", fg: "#FFFFFF" },
  GS_FRESH: { name: "GS더프레시", label: "GS", bg: "#00A651", fg: "#FFFFFF" },
  LOTTE_SUPER: { name: "롯데슈퍼", label: "LS", bg: "#ED1C24", fg: "#FFFFFF" },
  EMART_EVERYDAY: { name: "이마트 에브리데이", label: "ED", bg: "#FFD400", fg: "#231F20" },
  HOMEPLUS_EXPRESS: { name: "홈플러스 익스프레스", label: "HE", bg: "#E31837", fg: "#FFFFFF" },
  ETC: { name: "그 외 마트(동네 마트)", label: "마트", bg: "#F2F4F6", fg: "#4E5968" },
} as const satisfies Record<string, { name: string; label: string; bg: string; fg: string }>;

export type StoreCode = keyof typeof STORES;

export const STORE_CODES = Object.keys(STORES) as StoreCode[];

/** 모르는 코드나 null이면 ETC */
export function toStoreCode(code: string | null | undefined): StoreCode {
  return code != null && Object.hasOwn(STORES, code) ? (code as StoreCode) : "ETC";
}
