// 운영자 사이드 메뉴(피그마 운영자 페이지 > 사이드 메뉴)와 운영자 주소. 모니터링·운영 두 묶음.
// 주소는 blueprint §4 "Admin screens"(11/26): /admin/monitoring/*, /admin/operation/*

export type AdminMenuKey =
  "dashboard" | "funnel" | "feature-metrics" | "event-logs" | "reports" | "users" | "rankings";

/** a3 기능별 지표 탭(API 63 `tab`) */
export type MetricsTab =
  | "registration"
  | "consumption"
  | "ai"
  | "meal-plan-cart"
  | "shared"
  | "retention"
  | "cost"
  | "error";

export const ADMIN_PATHS = {
  /** /admin으로 들어오면 여기로 보낸다 */
  home: "/admin/monitoring/dashboard",
  dashboard: "/admin/monitoring/dashboard",
  funnels: "/admin/monitoring/funnels",
  metrics: (tab: MetricsTab) => `/admin/monitoring/metrics/${tab}`,
  events: "/admin/monitoring/events",
  reports: "/admin/operation/reports",
  users: "/admin/operation/users",
  weeklyRewards: "/admin/operation/weekly-rewards",
} as const;

export type AdminMenuItem = {
  key: AdminMenuKey;
  label: string;
  /** 메뉴를 누르면 가는 주소 */
  href: string;
  /** 지금 주소가 이것으로 시작하면 이 메뉴를 지금 메뉴로 표시한다(상세·탭 주소 포함) */
  basePath: string;
};

export type AdminMenuGroup = {
  label: string;
  items: AdminMenuItem[];
};

export const ADMIN_MENU_GROUPS: AdminMenuGroup[] = [
  {
    label: "모니터링",
    items: [
      {
        key: "dashboard",
        label: "대시보드",
        href: ADMIN_PATHS.dashboard,
        basePath: ADMIN_PATHS.dashboard,
      },
      { key: "funnel", label: "퍼널", href: ADMIN_PATHS.funnels, basePath: ADMIN_PATHS.funnels },
      {
        key: "feature-metrics",
        label: "기능별 지표",
        // 피그마 기본 화면(a3-기능별 지표)이 AI 탭이라 메뉴는 AI 탭으로 연다
        href: ADMIN_PATHS.metrics("ai"),
        basePath: "/admin/monitoring/metrics",
      },
      {
        key: "event-logs",
        label: "이벤트 로그",
        href: ADMIN_PATHS.events,
        basePath: ADMIN_PATHS.events,
      },
    ],
  },
  {
    label: "운영",
    items: [
      {
        key: "reports",
        label: "신고 관리",
        href: ADMIN_PATHS.reports,
        basePath: ADMIN_PATHS.reports,
      },
      { key: "users", label: "사용자 관리", href: ADMIN_PATHS.users, basePath: ADMIN_PATHS.users },
      {
        key: "rankings",
        label: "랭킹·보상",
        href: ADMIN_PATHS.weeklyRewards,
        basePath: ADMIN_PATHS.weeklyRewards,
      },
    ],
  },
];

/** 주소로 지금 메뉴를 찾는다. 운영자 주소가 아니면 undefined */
export function findActiveMenu(pathname: string): AdminMenuKey | undefined {
  for (const group of ADMIN_MENU_GROUPS) {
    for (const item of group.items) {
      if (pathname === item.basePath || pathname.startsWith(`${item.basePath}/`)) return item.key;
    }
  }
  return undefined;
}

/** "서비스로 돌아가기"가 가는 곳: 앱 첫 화면(스플래시가 로그인 여부에 따라 보냄, api-summary) */
export const SERVICE_HOME_PATH = "/";
