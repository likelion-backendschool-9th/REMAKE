"use client";

import { useState } from "react";

import { Badge } from "@/components/Badge/Badge";
import { EmptyState } from "@/components/EmptyState/EmptyState";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { TextButton } from "@/components/TextButton/TextButton";
import { formatWon } from "@/lib/format";

import { AdminCard } from "../../../../_components/AdminCard/AdminCard";
import { AdminLoadError } from "../../../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../../../_components/AdminPageHeader/AdminPageHeader";
import { AdminPeriodFilter } from "../../../../_components/AdminPeriodFilter/AdminPeriodFilter";
import { BarChart } from "../../../../_components/BarChart/BarChart";
import { ADMIN_PATHS } from "../../../../_lib/admin-menu";
import { type AdminPeriod, DEFAULT_ADMIN_PERIOD, getPeriodLabel } from "../../../../_lib/period";
import type { DashboardAnomaly, DashboardData, DashboardKpi } from "../../_lib/dashboard";

import styles from "./DashboardContainer.module.css";

type DashboardContainerProps = {
  /** 대시보드 값. 없으면 불러오는 중(a0-로딩). 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  dashboard?: DashboardData;
  /** 불러오기 실패(a0-불러오기 실패) */
  hasError?: boolean;
  /** 처음 고른 기간. 기본 최근 7일 */
  initialPeriod?: AdminPeriod;
};

/**
 * a1 대시보드. 이벤트로 모은 핵심 지표(북극성 지표 M00, 처리 대기 신고, 버린 식재료 금액 M18, KPI 8개)를 보인다.
 * 상태: 기본(a1-대시보드) / 이상 징후(a1-대시보드-이상 징후) / 데이터 없음(a1-대시보드-데이터 없음)
 * / 불러오는 중(a0-로딩) / 불러오기 실패(a0-불러오기 실패).
 * 기간·실제 데이터만 칩은 API가 붙으면 조회 조건으로 쓴다
 */
export function DashboardContainer({
  dashboard,
  hasError = false,
  initialPeriod = DEFAULT_ADMIN_PERIOD,
}: DashboardContainerProps) {
  const [period, setPeriod] = useState<AdminPeriod>(initialPeriod);
  const [excludeSeed, setExcludeSeed] = useState(true);

  return (
    <>
      <AdminPageHeader
        title="대시보드"
        description="이벤트로 모은 핵심 지표예요. 계산식은 이벤트 정의서 5장을 따라요."
        actions={
          <AdminPeriodFilter
            period={period}
            onPeriodChange={setPeriod}
            excludeSeed={excludeSeed}
            onExcludeSeedChange={setExcludeSeed}
          />
        }
      />
      {hasError ? (
        <AdminLoadError title="지표를 불러오지 못했어요" />
      ) : !dashboard ? (
        <DashboardSkeleton />
      ) : dashboard.isEmpty ? (
        <AdminCard title={getPeriodLabel(period)} className={styles.emptyCard}>
          <EmptyState>이 기간에 쌓인 이벤트가 없어요</EmptyState>
        </AdminCard>
      ) : (
        <>
          {dashboard.anomaly && <AnomalyBanner anomaly={dashboard.anomaly} />}
          <div className={styles.topRow}>
            <NorthStarCard northStar={dashboard.northStar} />
            <div className={styles.sideColumn}>
              <AdminCard title="처리 대기 신고">
                <p className={styles.bigNumber} data-tone="danger">
                  {dashboard.pendingReportCount}건
                </p>
                <p className={styles.assist}>자동으로 숨기지 않아요. 운영자가 확인해 처리해요.</p>
                <TextButton withArrow href={ADMIN_PATHS.reports} className={styles.link}>
                  신고 관리로 가기
                </TextButton>
              </AdminCard>
              <AdminCard title="이번 달 버린 식재료 금액" meta="M18">
                <p className={styles.mediumNumber}>
                  {dashboard.discardedCost.amount === null
                    ? "-"
                    : formatWon(dashboard.discardedCost.amount)}
                </p>
                {dashboard.discardedCost.unpricedCount > 0 && (
                  <p className={styles.assist}>
                    가격 없는 재료 {dashboard.discardedCost.unpricedCount}건은 따로 세요
                  </p>
                )}
              </AdminCard>
            </div>
          </div>
          <ul className={styles.kpiGrid}>
            {dashboard.kpis.map((kpi) => (
              <li key={kpi.code}>
                <KpiCard kpi={kpi} />
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  );
}

/** a1-대시보드-이상 징후: 페이지 제목 아래 빨간 테두리 띠 */
function AnomalyBanner({ anomaly }: { anomaly: DashboardAnomaly }) {
  return (
    <div className={styles.anomaly} role="alert">
      <Badge tone="danger">이상 징후</Badge>
      <p className={styles.anomalyMessage}>{anomaly.message}</p>
      {anomaly.detail && <p className={styles.anomalyDetail}>{anomaly.detail}</p>}
      <TextButton withArrow href={ADMIN_PATHS.metrics(anomaly.metricsTab)}>
        기능별 지표 보기
      </TextButton>
    </div>
  );
}

/** 북극성 지표 M00: 소비기한 내 소진율과 주별 막대그래프 */
function NorthStarCard({ northStar }: { northStar: DashboardData["northStar"] }) {
  const { value, deltaPoint, weekly } = northStar;
  return (
    <AdminCard
      title="북극성 지표 · 소비기한 내 소진율"
      meta="M00 · 사용 ÷ (사용 + 버림 + 기한 지남)"
      className={styles.northStar}
    >
      <div className={styles.northStarValue}>
        <p className={styles.bigNumber}>{value === null ? "-" : `${value}%`}</p>
        {deltaPoint !== null && deltaPoint !== 0 && (
          <p className={styles.delta} data-tone={deltaPoint > 0 ? "up" : "down"}>
            지난주보다 {Math.abs(deltaPoint)}%p {deltaPoint > 0 ? "올랐어요" : "내렸어요"}
          </p>
        )}
      </div>
      {weekly.length > 0 && (
        <div className={styles.chart}>
          <BarChart
            items={weekly}
            aria-label="주별 소비기한 내 소진율"
            formatValue={(weekValue) => String(weekValue)}
          />
        </div>
      )}
    </AdminCard>
  );
}

/** KPI 카드. 값이 없으면 "-"를 보이고 목표 배지는 숨긴다 */
function KpiCard({ kpi }: { kpi: DashboardKpi }) {
  return (
    <article className={styles.kpi}>
      <div className={styles.kpiHeader}>
        <h2 className={styles.kpiName}>{kpi.name}</h2>
        {kpi.value !== null && kpi.status && (
          <Badge tone={kpi.status === "achieved" ? "safe" : "danger"}>
            {kpi.status === "achieved" ? "달성" : "미달"}
          </Badge>
        )}
      </div>
      <p className={styles.mediumNumber}>{kpi.value === null ? "-" : `${kpi.value}%`}</p>
      <p className={styles.assist}>
        {kpi.code} · {kpi.goal}
      </p>
    </article>
  );
}

/** a0-로딩: 대시보드 카드 자리에 스켈레톤 블록 */
function DashboardSkeleton() {
  return (
    <div className={styles.skeleton} aria-busy="true">
      <span className={styles.srOnly} role="status">
        지표를 불러오는 중이에요
      </span>
      <div className={styles.topRow}>
        <div className={styles.skeletonCard} data-grow="true">
          <Skeleton width="15rem" height="1.125rem" />
          <Skeleton width="8.75rem" height="2.25rem" />
          <Skeleton width="100%" height="6.875rem" />
        </div>
        <div className={styles.skeletonCard} data-side="true">
          <Skeleton width="7.5rem" height="1.125rem" />
          <Skeleton width="5.625rem" height="2.25rem" />
          <Skeleton width="16.25rem" height="0.875rem" />
          <Skeleton width="7.5rem" height="1.125rem" />
          <Skeleton width="8.75rem" height="1.875rem" />
        </div>
      </div>
      <div className={styles.kpiGrid}>
        {Array.from({ length: 8 }, (_, index) => (
          <div key={index} className={styles.skeletonCard} data-kpi="true">
            <Skeleton width="9.375rem" height="1rem" />
            <Skeleton width="5rem" height="1.75rem" />
            <Skeleton width="6.875rem" height="0.75rem" />
          </div>
        ))}
      </div>
    </div>
  );
}
