// 알림 화면(p6) 값. 모양은 API 44(GET /user/notifications) 응답 content 한 줄을 따랐다.
// TODO: API 44를 붙이면 서버 값을 쓴다(20개씩, hasNext면 이어 불러오기). 목록을 불러오면 서버가 모두 읽음 처리한다.

import { dayjs } from "@/lib/dayjs";
import { formatDaysAgo } from "@/lib/format";

export type NotificationType =
  "EXPIRY_SOON" | "EXPIRED" | "MEAL_PLAN_COMPLETED" | "MEAL_PLAN_FAILED";

export type NotificationItem = {
  id: number;
  type: NotificationType;
  /** 서버가 만든 문구 그대로 */
  message: string;
  /** MEAL_PLAN_COMPLETED만 있음 */
  mealPlanId: number | null;
  /** ISO 날짜·시각 */
  createdAt: string;
};

export type NotificationGroup = { label: string; items: NotificationItem[] };

/** 최신순 목록을 "오늘", "1일 전", "2일 전" … 묶음으로 나눈다(순서 유지) */
export function groupByDay(items: readonly NotificationItem[], now: Date): NotificationGroup[] {
  const groups: NotificationGroup[] = [];
  for (const item of items) {
    const label = formatDaysAgo(item.createdAt, now);
    const last = groups.at(-1);
    if (last?.label === label) last.items.push(item);
    else groups.push({ label, items: [item] });
  }
  return groups;
}

/**
 * 누르면 갈 곳(domain-rules 6). 소비기한 알림은 누르는 동작이 없다.
 * 식단표 완성은 p5에서 그 식단표 시작일, 실패는 식단표 생성 설정.
 * TODO: p5를 만들 때 mealPlanId로 시작일을 여는 주소 형식을 맞춘다
 */
export function getNotificationHref(item: NotificationItem): string | null {
  if (item.type === "MEAL_PLAN_COMPLETED" && item.mealPlanId !== null) {
    return `/user/meal-plan/calendar?mealPlanId=${item.mealPlanId}`;
  }
  if (item.type === "MEAL_PLAN_FAILED") return "/user/meal-plan/meal-plans/new";
  return null;
}

/** 마크업 확인용 값(피그마 p6-알람과 같은 문구 + 식단표 알림 2개). 미리보기(app/dev)에서만 쓴다 */
export function createSampleNotifications(now: Date): NotificationItem[] {
  const at = (daysAgo: number, hour: number) =>
    dayjs(now).subtract(daysAgo, "day").hour(hour).minute(0).second(0).toISOString();
  return [
    {
      id: 7,
      type: "MEAL_PLAN_COMPLETED",
      message: "11월 24일부터 5일치 식단표가 완성됐어요",
      mealPlanId: 12,
      createdAt: at(0, 9),
    },
    {
      id: 6,
      type: "EXPIRY_SOON",
      message: "우유의 소비기한이 얼마 남지 않았어요. 제품의 포장지에서 사용 기한을 확인해 주세요",
      mealPlanId: null,
      createdAt: at(0, 9),
    },
    {
      id: 5,
      type: "EXPIRED",
      message: "기한이 지난 재료 2개가 있어요. 냉장고를 확인해 주세요",
      mealPlanId: null,
      createdAt: at(0, 9),
    },
    {
      id: 4,
      type: "EXPIRY_SOON",
      message: "우유의 소비기한이 얼마 남지 않았어요. 제품의 포장지에서 사용 기한을 확인해 주세요",
      mealPlanId: null,
      createdAt: at(1, 9),
    },
    {
      id: 3,
      type: "MEAL_PLAN_FAILED",
      message:
        "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요",
      mealPlanId: null,
      createdAt: at(1, 9),
    },
    {
      id: 2,
      type: "EXPIRY_SOON",
      message: "만두의 소비기한이 얼마 남지 않았어요. 제품의 포장지에서 사용 기한을 확인해 주세요",
      mealPlanId: null,
      createdAt: at(2, 9),
    },
    {
      id: 1,
      type: "EXPIRED",
      message: "기한이 지난 재료 2개가 있어요. 냉장고를 확인해 주세요",
      mealPlanId: null,
      createdAt: at(2, 9),
    },
  ];
}
