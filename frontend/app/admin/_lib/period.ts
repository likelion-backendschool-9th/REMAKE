// 운영자 지표 화면의 기간 칩(피그마 a1·a2 페이지 제목 오른쪽). 값은 모니터링 API(61~63)의 `period`와 같다(api-summary Common)

export type AdminPeriod = "TODAY" | "LAST_7_DAYS" | "LAST_30_DAYS";

/** 기간을 안 고르면 최근 7일(API 기본값) */
export const DEFAULT_ADMIN_PERIOD: AdminPeriod = "LAST_7_DAYS";

export const ADMIN_PERIODS: { value: AdminPeriod; label: string }[] = [
  { value: "TODAY", label: "오늘" },
  { value: "LAST_7_DAYS", label: "최근 7일" },
  { value: "LAST_30_DAYS", label: "최근 30일" },
];

export function getPeriodLabel(period: AdminPeriod): string {
  return ADMIN_PERIODS.find((item) => item.value === period)?.label ?? "";
}
