// a1 대시보드 화면 값. 어드민 집계 API(#93)가 보류라 모양은 피그마 기준으로 잡았고, API가 정해지면 맞춘다.
// 지표 계산·목표 달성 여부는 서버가 정한다(blueprint §1). 화면은 받은 값을 그대로 보인다

import type { BarChartItem } from "../_components/BarChart/BarChart";

export type KpiStatus = "achieved" | "missed";

export type DashboardKpi = {
  /** 지표 번호(이벤트 정의서 5장). 예: "M01" */
  code: string;
  name: string;
  /** 퍼센트 값. 계산할 분모가 0이거나 이벤트가 없으면 null → "-"를 보이고 배지는 숨긴다 */
  value: number | null;
  /** 예: "목표 70% 이상" */
  goal: string;
  status: KpiStatus | null;
};

export type DashboardAnomaly = {
  /** 예: "영수증 OCR 실패율이 최근 1시간 18%예요(평소 8%)." */
  message: string;
  /** 오른쪽 보조 글. 예: "신고 1시간 6건(평소 1건)" */
  detail?: string;
};

export type DashboardData = {
  /** 고른 기간에 쌓인 이벤트가 하나도 없으면 true(a1-대시보드-데이터 없음) */
  isEmpty: boolean;
  /** 이상 징후가 있으면 페이지 제목 아래 빨간 띠로 보인다(a1-대시보드-이상 징후) */
  anomaly: DashboardAnomaly | null;
  /** M00 소비기한 내 소진율 */
  northStar: {
    value: number | null;
    /** 지난주 대비 %p. 양수=올랐어요, 음수=내렸어요 */
    deltaPoint: number | null;
    /** 주별 값(오래된 주 → 이번 주) */
    weekly: BarChartItem[];
  };
  pendingReportCount: number;
  /** M18 이번 달 버린 식재료 금액(원) */
  discardedCost: {
    amount: number | null;
    /** 가격 없는 재료 수(금액에 넣지 않고 따로 센다) */
    unpricedCount: number;
  };
  kpis: DashboardKpi[];
};

/** 개발용 미리보기(app/dev)에서만 쓰는 피그마 예시 값 */
export const SAMPLE_DASHBOARD: DashboardData = {
  isEmpty: false,
  anomaly: null,
  northStar: {
    value: 68.4,
    deltaPoint: 3.1,
    weekly: [
      { label: "10/6", value: 52.1 },
      { label: "10/13", value: 54.9 },
      { label: "10/20", value: 57.7 },
      { label: "10/27", value: 57 },
      { label: "11/3", value: 61.3 },
      { label: "11/10", value: 62.7 },
      { label: "11/17", value: 65.6 },
      { label: "11/24", value: 68.4 },
    ],
  },
  pendingReportCount: 12,
  discardedCost: { amount: 48200, unpricedCount: 7 },
  kpis: [
    {
      code: "M01",
      name: "로그인 → 온보딩 완료율",
      value: 74,
      goal: "목표 70% 이상",
      status: "achieved",
    },
    {
      code: "M02",
      name: "첫 재료 등록률(24시간 내)",
      value: 58,
      goal: "목표 60% 이상",
      status: "missed",
    },
    {
      code: "M03",
      name: "영수증 OCR 성공률",
      value: 92.3,
      goal: "목표 90% 이상",
      status: "achieved",
    },
    { code: "M04", name: "OCR 품목 수정률", value: 17, goal: "목표 20% 이하", status: "achieved" },
    { code: "M06", name: "임박 알림 확인율", value: 31, goal: "목표 25% 이상", status: "achieved" },
    {
      code: "M08",
      name: "레시피 생성 → 요리 완료율",
      value: 27,
      goal: "목표 30% 이상",
      status: "missed",
    },
    {
      code: "M09",
      name: "AI 레시피 생성 성공률",
      value: 96.9,
      goal: "목표 95% 이상",
      status: "achieved",
    },
    { code: "M15", name: "D7 리텐션", value: 22, goal: "목표 25% 이상", status: "missed" },
  ],
};

export const SAMPLE_ANOMALY: DashboardAnomaly = {
  message: "영수증 OCR 실패율이 최근 1시간 18%예요(평소 8%).",
  detail: "신고 1시간 6건(평소 1건)",
};

export const EMPTY_DASHBOARD: DashboardData = {
  isEmpty: true,
  anomaly: null,
  northStar: { value: null, deltaPoint: null, weekly: [] },
  pendingReportCount: 0,
  discardedCost: { amount: null, unpricedCount: 0 },
  kpis: [],
};
