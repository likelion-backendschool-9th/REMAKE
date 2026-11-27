// 운영자 페이지(a0~a7) 화면 상태 목록. /dev/admin이 이 순서로 목록을 보이고, /dev/admin/[state]가 하나를 그린다.
// 상태를 더하면 여기에 한 줄 추가한다(blueprint §3.1 7번). 실제 라우트는 컨테이너 기본값(불러오는 중)을 쓰고, 마크업 확인용 값은 여기서만 넘긴다.
// 운영자 주소는 blueprint §4 "Admin screens". a0 상태는 각 운영자 화면 안 상태라 대시보드 주소로 적는다

import type { ReactNode } from "react";

import { AdminForbidden } from "../../../admin/_components/AdminForbidden/AdminForbidden";
import { AdminLayout } from "../../../admin/_components/AdminLayout/AdminLayout";
import { ADMIN_PATHS, type AdminMenuKey } from "../../../admin/_lib/admin-menu";
import { DashboardContainer } from "../../../admin/monitoring/dashboard/_components/DashboardContainer/DashboardContainer";
import {
  EMPTY_DASHBOARD,
  SAMPLE_ANOMALY,
  SAMPLE_DASHBOARD,
} from "../../../admin/monitoring/dashboard/_lib/dashboard";
import { EventLogContainer } from "../../../admin/monitoring/events/_components/EventLogContainer/EventLogContainer";
import { SAMPLE_EVENT_LOGS } from "../../../admin/monitoring/events/_lib/event-log";
import { FunnelContainer } from "../../../admin/monitoring/funnels/_components/FunnelContainer/FunnelContainer";
import { SAMPLE_FUNNEL } from "../../../admin/monitoring/funnels/_lib/funnel";
import { UserManagementContainer } from "../../../admin/operation/users/_components/UserManagementContainer/UserManagementContainer";
import { WeeklyRewardsContainer } from "../../../admin/operation/weekly-rewards/_components/WeeklyRewardsContainer/WeeklyRewardsContainer";
import type { PreviewState } from "../../_lib/preview";
import { EMPTY_REWARDS, FEWER_REWARDS, SAMPLE_REWARDS, SAMPLE_USERS } from "./operation-samples";

/** 미리보기 주소(/dev/admin/...)로는 지금 메뉴를 알 수 없어 넘긴다. 피그마처럼 "신고 관리" 옆에 처리 대기 12건을 보인다 */
function withLayout(activeMenu: AdminMenuKey, content: ReactNode) {
  return (
    <AdminLayout
      activeMenu={activeMenu}
      pendingReportCount={SAMPLE_DASHBOARD.pendingReportCount}
      menuHrefs={{
        dashboard: "/dev/admin/a1-dashboard",
        funnel: "/dev/admin/a2-funnel",
        "event-logs": "/dev/admin/a6-event-logs",
        users: "/dev/admin/a5-users",
        rankings: "/dev/admin/a7-rewards",
        "feature-metrics": null,
        reports: null,
      }}
    >
      {content}
    </AdminLayout>
  );
}

export const ADMIN_STATES: PreviewState[] = [
  {
    slug: "a0-forbidden",
    label: "a0-권한 없음",
    route: ADMIN_PATHS.dashboard,
    render: () => <AdminForbidden />,
  },
  {
    slug: "a0-loading",
    label: "a0-로딩 (실제 라우트 처음 상태)",
    route: ADMIN_PATHS.dashboard,
    render: () => withLayout("dashboard", <DashboardContainer />),
  },
  {
    slug: "a0-load-error",
    label: "a0-불러오기 실패",
    route: ADMIN_PATHS.dashboard,
    render: () => withLayout("dashboard", <DashboardContainer hasError />),
  },
  {
    slug: "a1-dashboard",
    label: "a1-대시보드",
    route: ADMIN_PATHS.dashboard,
    render: () => withLayout("dashboard", <DashboardContainer dashboard={SAMPLE_DASHBOARD} />),
  },
  {
    slug: "a1-dashboard-anomaly",
    label: "a1-대시보드-이상 징후",
    route: ADMIN_PATHS.dashboard,
    render: () =>
      withLayout(
        "dashboard",
        <DashboardContainer dashboard={{ ...SAMPLE_DASHBOARD, anomaly: SAMPLE_ANOMALY }} />,
      ),
  },
  {
    slug: "a1-dashboard-empty",
    label: "a1-대시보드-데이터 없음",
    route: ADMIN_PATHS.dashboard,
    render: () =>
      withLayout(
        "dashboard",
        <DashboardContainer dashboard={EMPTY_DASHBOARD} initialPeriod="TODAY" />,
      ),
  },
  {
    slug: "a2-funnel",
    label: "a2-퍼널",
    route: ADMIN_PATHS.funnels,
    render: () => withLayout("funnel", <FunnelContainer funnel={SAMPLE_FUNNEL} />),
  },
  {
    slug: "a6-event-logs",
    label: "a6-이벤트 로그",
    route: ADMIN_PATHS.events,
    render: () => withLayout("event-logs", <EventLogContainer events={SAMPLE_EVENT_LOGS} />),
  },
  {
    slug: "a6-event-logs-empty",
    label: "a6-이벤트 로그-검색 결과 없음",
    route: ADMIN_PATHS.events,
    render: () =>
      withLayout(
        "event-logs",
        <EventLogContainer events={[]} initialEventName="recipe_generatd" />,
      ),
  },
  {
    slug: "a5-users",
    label: "a5-사용자 관리",
    route: ADMIN_PATHS.users,
    render: () => withLayout("users", <UserManagementContainer users={SAMPLE_USERS} />),
  },
  {
    slug: "a5-users-adjust",
    label: "a5-사용자 관리-생성 횟수 조정",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout(
        "users",
        <UserManagementContainer
          users={SAMPLE_USERS}
          initialModal="adjust"
          initialReason="금주의 레시피 보상 누락 보정"
        />,
      ),
  },
  {
    slug: "a5-users-reason-empty",
    label: "a5-사용자 관리-생성 횟수 조정-사유 비움",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout(
        "users",
        <UserManagementContainer users={SAMPLE_USERS} initialModal="reason-empty" />,
      ),
  },
  {
    slug: "a5-users-empty",
    label: "a5-사용자 관리-검색 결과 없음",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout("users", <UserManagementContainer users={SAMPLE_USERS} initialSearch="#99999" />),
  },
  {
    slug: "a5-users-sanctioned",
    label: "a5-사용자 관리-제재 중",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout(
        "users",
        <UserManagementContainer users={SAMPLE_USERS} initialFilter="SANCTIONED" />,
      ),
  },
  {
    slug: "a5-users-no-sanctions",
    label: "a5-사용자 관리-제재 없는 사용자",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout(
        "users",
        <UserManagementContainer users={SAMPLE_USERS} initialSelectedId={10233} />,
      ),
  },
  {
    slug: "a5-users-permanent",
    label: "a5-사용자 관리-영구 정지",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout(
        "users",
        <UserManagementContainer users={SAMPLE_USERS} initialSelectedId={10501} />,
      ),
  },
  {
    slug: "a5-users-withdrawn",
    label: "a5-사용자 관리-탈퇴한 사용자",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout("users", <UserManagementContainer users={SAMPLE_USERS} initialSearch="#10044" />),
  },
  {
    slug: "a5-users-adjusted",
    label: "a5-사용자 관리-조정 완료 토스트",
    route: ADMIN_PATHS.users,
    render: () =>
      withLayout(
        "users",
        <UserManagementContainer
          users={SAMPLE_USERS.map((user) =>
            user.userId === 10087
              ? { ...user, quota: { ...user.quota, rewardRemaining: 8 } }
              : user,
          )}
          initialToast="생성 횟수를 2회 늘렸어요"
        />,
      ),
  },
  {
    slug: "a7-rewards",
    label: "a7-랭킹·보상",
    route: ADMIN_PATHS.weeklyRewards,
    render: () => withLayout("rankings", <WeeklyRewardsContainer data={SAMPLE_REWARDS} />),
  },
  {
    slug: "a7-rewards-empty",
    label: "a7-랭킹·보상-보상 없음",
    route: ADMIN_PATHS.weeklyRewards,
    render: () => withLayout("rankings", <WeeklyRewardsContainer data={EMPTY_REWARDS} />),
  },
  {
    slug: "a7-rewards-fewer",
    label: "a7-랭킹·보상-3개 미만",
    route: ADMIN_PATHS.weeklyRewards,
    render: () => withLayout("rankings", <WeeklyRewardsContainer data={FEWER_REWARDS} />),
  },
];
