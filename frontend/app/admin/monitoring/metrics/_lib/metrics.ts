// a3 기능별 지표 화면 값. 모양은 피그마 기준이고, API 63 `GET /api/v1/admin/metrics/{tab}` 응답
// `{ cards, tables, daily }`에 맞춰 연동할 때 이름을 맞춘다.
// 지표 계산·목표 달성 여부는 서버가 정한다(blueprint §1). 화면은 받은 값을 그대로 보인다

import type {
  AdminTableCell,
  AdminTableColumn,
} from "../../../_components/AdminTableCard/AdminTableCard";
import type { BarChartItem } from "../../../_components/BarChart/BarChart";
import type { MetricsTab } from "../../../_lib/admin-menu";

/** 피그마 탭 순서. 주소 조각(`MetricsTab`)과 탭 이름 */
export const METRICS_TABS: readonly { tab: MetricsTab; label: string }[] = [
  { tab: "registration", label: "등록" },
  { tab: "consumption", label: "소비" },
  { tab: "ai", label: "AI" },
  { tab: "meal-plan-cart", label: "식단표·장바구니" },
  { tab: "shared", label: "공유" },
  { tab: "retention", label: "리텐션" },
  { tab: "cost", label: "비용" },
  { tab: "error", label: "오류" },
];

export function isMetricsTab(value: string): value is MetricsTab {
  return METRICS_TABS.some((item) => item.tab === value);
}

/** 달성=목표를 지킴, 추세=목표 없이 오르내림만 봄, 미달=목표를 못 지킴 */
export type MetricStatus = "achieved" | "trend" | "missed";

export type MetricCardData = {
  /** 지표 번호(이벤트 정의서 5장). 예: "M03" */
  code: string;
  name: string;
  /** 보일 값 글(예: "92.3%", "6.2초", "1,284명"). 계산할 분모가 0이거나 이벤트가 없으면 null → "-"를 보이고 배지는 숨긴다 */
  value: string | null;
  /** 지표 번호 뒤 보조 글. 예: "목표 90% 이상" */
  caption: string;
  status: MetricStatus | null;
};

export type MetricCell = AdminTableCell;
export type MetricColumn = AdminTableColumn;

export type MetricTableData = {
  key: string;
  title: string;
  /** 제목 오른쪽 작은 글(지표 번호·집계 기준) */
  meta?: string;
  columns: MetricColumn[];
  rows: Record<string, MetricCell>[];
  /** 표 아래 보조 글 */
  note?: string;
};

export type MetricDailyData = {
  title: string;
  meta?: string;
  /** 오래된 날 → 오늘. 마지막 막대만 진하게 값을 보인다 */
  items: BarChartItem[];
  /** 막대그래프 옆에 세로로 놓는 요약(AI 탭) */
  summary?: { label: string; value: string }[];
  "aria-label": string;
};

export type MetricsTabData = {
  cards: MetricCardData[];
  tables: MetricTableData[];
  daily: MetricDailyData;
};
