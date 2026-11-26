// 운영자 지표 화면의 기간 칩(피그마 a1·a2 페이지 제목 오른쪽). 값 이름은 API가 정해지면 맞춘다

export type AdminPeriod = "today" | "last7days" | "last30days";

export const ADMIN_PERIODS: { value: AdminPeriod; label: string }[] = [
  { value: "today", label: "오늘" },
  { value: "last7days", label: "최근 7일" },
  { value: "last30days", label: "최근 30일" },
];

export function getPeriodLabel(period: AdminPeriod): string {
  return ADMIN_PERIODS.find((item) => item.value === period)?.label ?? "";
}
