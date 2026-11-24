// p5(식단표)·p5.1(식단표 생성) 화면 상태 목록. /dev/p5가 이 순서로 목록을 보이고, /dev/p5/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값(불러오는 중)을 쓰고, 마크업 확인용 값은 여기서만 넘긴다.

import { MealPlanContainer } from "../../../user/meal-plan/calendar/_components/MealPlanContainer/MealPlanContainer";
import { SAMPLE_CALENDAR, SAMPLE_SLOTS } from "../../../user/meal-plan/calendar/_lib/calendar";
import { MealPlanSetupContainer } from "../../../user/meal-plan/meal-plans/new/_components/MealPlanSetupContainer/MealPlanSetupContainer";
import type { PreviewState } from "../../_lib/preview";

const ROUTE = "/user/meal-plan/calendar";
const NOTICE = { message: "당근의 소비기한이 임박했어요!", unreadCount: 2 };
const TODAY = "2026-11-17";
const SETUP_ROUTE = "/user/meal-plan/meal-plans/new";
const SETUP_DEFAULTS = { remainingPlans: 1, householdSize: 1, lastPurpose: null };
const SLOTS = { [TODAY]: SAMPLE_SLOTS, "2026-11-16": SAMPLE_SLOTS };
const BASE = {
  calendar: SAMPLE_CALENDAR,
  slotsByDate: SLOTS,
  remainingPlans: 1,
  notice: NOTICE,
  today: TODAY,
};

export const P5_STATES: PreviewState[] = [
  {
    slug: "calendar",
    label: "p5-식단표",
    route: ROUTE,
    render: () => <MealPlanContainer {...BASE} />,
  },
  {
    slug: "calendar-expanded",
    label: "p5-식단표-바텀 시트 높이 조절",
    route: ROUTE,
    render: () => <MealPlanContainer {...BASE} initialExpanded />,
  },
  {
    slug: "calendar-menu",
    label: "p5-식단표-케밥 클릭 (참치 김치찌개)",
    route: ROUTE,
    render: () => <MealPlanContainer {...BASE} initialSlotMenuId={101} />,
  },
  {
    slug: "calendar-month",
    label: "p5-식단표-날짜 수정 (캘린더 선택)",
    route: ROUTE,
    render: () => <MealPlanContainer {...BASE} initialSheet="month" />,
  },
  {
    slug: "calendar-add",
    label: "p5-식단표-식단표 추가",
    route: ROUTE,
    render: () => <MealPlanContainer {...BASE} initialSheet="add" />,
  },
  {
    slug: "calendar-day-empty",
    label: "p5-식단표-바텀 시트-빈 리스트 (11월 26일)",
    route: ROUTE,
    render: () => <MealPlanContainer {...BASE} initialDate="2026-11-26" />,
  },
  {
    slug: "calendar-past",
    label: "p5-식단표-지난 날 (11월 16일, ⋮ 누르면 토스트, 피그마 없음)",
    route: ROUTE,
    render: () => <MealPlanContainer {...BASE} initialDate="2026-11-16" />,
  },
  {
    slug: "calendar-no-plan-month",
    label: "p5-식단표-식단 없는 달 (2026년 10월, 피그마 5891-6495)",
    route: ROUTE,
    render: () => (
      <MealPlanContainer
        {...BASE}
        calendar={{ month: "2026-10", periods: [], datesWithMeals: [] }}
        initialDate="2026-10-01"
      />
    ),
  },
  {
    slug: "calendar-gap-day",
    label: "p5-식단표-기간 안 식단 없는 날 (11월 20일 비움, 띠 끊김)",
    route: ROUTE,
    render: () => (
      <MealPlanContainer
        {...BASE}
        calendar={{
          ...SAMPLE_CALENDAR,
          datesWithMeals: SAMPLE_CALENDAR.datesWithMeals.filter((date) => date !== "2026-11-20"),
        }}
      />
    ),
  },
  {
    slug: "calendar-cross-month",
    label: "p5-식단표-달을 넘는 식단 (11/28~12/3, 12월 달력, 피그마 없음)",
    route: ROUTE,
    render: () => (
      <MealPlanContainer
        {...BASE}
        calendar={{
          month: "2026-12",
          periods: [{ mealPlanId: 14, startDate: "2026-11-28", endDate: "2026-12-03" }],
          datesWithMeals: ["2026-12-01", "2026-12-02", "2026-12-03"],
        }}
        initialDate="2026-12-02"
      />
    ),
  },
  {
    slug: "calendar-empty",
    label: "p5-빈 리스트 (식단표를 한 번도 만들지 않음)",
    route: ROUTE,
    render: () => (
      <MealPlanContainer
        {...BASE}
        calendar={{ month: "2026-11", periods: [], datesWithMeals: [] }}
        slotsByDate={{}}
        hasMealPlans={false}
      />
    ),
  },
  {
    slug: "calendar-loading",
    label: "p5-식단표-불러오는 중 (실제 라우트 처음 상태, 피그마 없음)",
    route: ROUTE,
    render: () => <MealPlanContainer notice={NOTICE} />,
  },
  {
    slug: "setup",
    label: "p5.1-식단표 생성",
    route: SETUP_ROUTE,
    render: () => (
      <MealPlanSetupContainer defaults={SETUP_DEFAULTS} initialStartDate="2026-11-18" />
    ),
  },
  {
    slug: "setup-start-date",
    label: "p5.1-식단표 생성-시작일",
    route: SETUP_ROUTE,
    render: () => (
      <MealPlanSetupContainer
        defaults={SETUP_DEFAULTS}
        initialStartDate="2026-11-18"
        initialSheet="startDate"
      />
    ),
  },
  {
    slug: "setup-purpose",
    label: "p5.1-식단표 생성-식단 목적",
    route: SETUP_ROUTE,
    render: () => (
      <MealPlanSetupContainer
        defaults={SETUP_DEFAULTS}
        initialStartDate="2026-11-18"
        initialSheet="purpose"
      />
    ),
  },
  {
    slug: "setup-meals",
    label: "p5.1-식단표 생성-넣을 끼니",
    route: SETUP_ROUTE,
    render: () => (
      <MealPlanSetupContainer
        defaults={SETUP_DEFAULTS}
        initialStartDate="2026-11-18"
        initialSheet="meals"
      />
    ),
  },
  {
    slug: "setup-overlap",
    label: "p5.1-식단표 생성-겹침 경고 (피그마 없음)",
    route: SETUP_ROUTE,
    render: () => (
      <MealPlanSetupContainer
        defaults={SETUP_DEFAULTS}
        initialStartDate="2026-11-18"
        initialOverlapOpen
      />
    ),
  },
  {
    slug: "setup-no-quota",
    label: "p5.1-식단표 생성-남은 횟수 0 (버튼 꺼짐, 피그마 없음)",
    route: SETUP_ROUTE,
    render: () => (
      <MealPlanSetupContainer
        defaults={{ ...SETUP_DEFAULTS, remainingPlans: 0 }}
        initialStartDate="2026-11-18"
      />
    ),
  },
  {
    slug: "setup-loading",
    label: "p5.1-식단표 생성-불러오는 중 (실제 라우트 처음 상태, 피그마 없음)",
    route: SETUP_ROUTE,
    render: () => <MealPlanSetupContainer />,
  },
];
