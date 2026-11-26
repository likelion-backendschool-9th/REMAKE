// 운영자 사이드 메뉴(피그마 운영자 페이지 > 사이드 메뉴). 모니터링·운영 두 묶음.
// 운영자 프론트 URL은 아직 정하지 않았다(#283). 정해지면 href를 채운다. 그 전까지 메뉴는 이동하지 않는다

export type AdminMenuKey =
  "dashboard" | "funnel" | "feature-metrics" | "event-logs" | "reports" | "users" | "rankings";

export type AdminMenuItem = {
  key: AdminMenuKey;
  label: string;
  /** 운영자 URL이 정해지면 채운다 */
  href?: string;
};

export type AdminMenuGroup = {
  label: string;
  items: AdminMenuItem[];
};

export const ADMIN_MENU_GROUPS: AdminMenuGroup[] = [
  {
    label: "모니터링",
    items: [
      { key: "dashboard", label: "대시보드" },
      { key: "funnel", label: "퍼널" },
      { key: "feature-metrics", label: "기능별 지표" },
      { key: "event-logs", label: "이벤트 로그" },
    ],
  },
  {
    label: "운영",
    items: [
      { key: "reports", label: "신고 관리" },
      { key: "users", label: "사용자 관리" },
      { key: "rankings", label: "랭킹·보상" },
    ],
  },
];

/** "서비스로 돌아가기"가 가는 곳: 로그인 뒤 첫 화면(냉장고, blueprint §4) */
export const SERVICE_HOME_PATH = "/user/fridge/ingredients";
