// 화면 표시 형식. 날짜·금액은 이 함수로만 그린다(blueprint §10 Display formats).
// API는 날짜를 "2026-11-19", 시각을 ISO 8601(+09:00), 금액을 원 단위 정수로 보낸다.
// 소비기한 D-N·D-DAY·폐기-N은 서버가 주는 dDay 그대로 보인다(여기서 계산하지 않음).

import { dayjs } from "@/lib/dayjs";

type DateInput = string | Date;

const wonFormatter = new Intl.NumberFormat("ko-KR");

/** 12345 → "12,345원" */
export function formatWon(amount: number): string {
  return `${wonFormatter.format(amount)}원`;
}

/** 금액만(단위 없이) 쉼표를 넣는다. 입력칸 옆에 "원"이 따로 있을 때. 12345 → "12,345" */
export function formatNumber(value: number): string {
  return wonFormatter.format(value);
}

/** "2026-11-01" → "2026년 11월" (영수증 월 선택, 소비 패턴 월 탭) */
export function formatYearMonth(date: DateInput): string {
  return dayjs(date).format("YYYY년 M월");
}

/** "2026-11-22" → "11월 22일" */
export function formatMonthDay(date: DateInput): string {
  return dayjs(date).format("M월 D일");
}

/** "2026-11-22" → "2026년 11월 22일" */
export function formatFullDate(date: DateInput): string {
  return dayjs(date).format("YYYY년 M월 D일");
}

/** ("2026-11-22", "2026-11-29") → "11월 22일~11월 29일" (영수증 주차 묶음) */
export function formatDateRange(start: DateInput, end: DateInput): string {
  return `${formatMonthDay(start)}~${formatMonthDay(end)}`;
}

/** 조리 시간: 45 → "45분", 60 → "1시간", 90 → "1시간 30분" (60분부터 시간 표기) */
export function formatCookingTime(minutes: number): string {
  if (minutes < 60) return `${minutes}분`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest === 0 ? `${hours}시간` : `${hours}시간 ${rest}분`;
}

/** 인분: 2 → "2인분" */
export function formatServings(servings: number): string {
  return `${servings}인분`;
}

/**
 * 알림 묶음 제목: 오늘이면 "오늘", 지난 날이면 "N일 전". 날짜만 비교한다.
 * 화면 묶음 표시용이며 소비기한처럼 서버가 정하는 값에는 쓰지 않는다.
 */
export function formatDaysAgo(date: DateInput, now: DateInput = new Date()): string {
  const days = dayjs(now).startOf("day").diff(dayjs(date).startOf("day"), "day");
  return days <= 0 ? "오늘" : `${days}일 전`;
}
