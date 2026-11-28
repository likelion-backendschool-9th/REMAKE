"use client";

import { Skeleton } from "@/components/Skeleton/Skeleton";

import { AdminCard } from "../../../../_components/AdminCard/AdminCard";
import { AdminLoadError } from "../../../../_components/AdminLoadError/AdminLoadError";
import { AdminPageHeader } from "../../../../_components/AdminPageHeader/AdminPageHeader";
import { AdminPeriodFilter } from "../../../../_components/AdminPeriodFilter/AdminPeriodFilter";
import { AdminTableCard } from "../../../../_components/AdminTableCard/AdminTableCard";
import { useAdminPeriodQuery } from "../../../../_hooks/useAdminPeriodQuery";
import type { MetricsTab } from "../../../../_lib/admin-menu";
import { type AdminPeriod, DEFAULT_ADMIN_PERIOD } from "../../../../_lib/period";
import type { MetricsTabData } from "../../_lib/metrics";
import { MetricCard } from "../MetricCard/MetricCard";
import { MetricChartCard } from "../MetricChartCard/MetricChartCard";
import { MetricsTabs } from "../MetricsTabs/MetricsTabs";

import styles from "./MetricsContainer.module.css";

type MetricsContainerProps = {
  tab: MetricsTab;
  /** 탭 값. 없으면 불러오는 중(a0-로딩). 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  metrics?: MetricsTabData;
  /** 불러오기 실패(a0-불러오기 실패) */
  hasError?: boolean;
  /** 처음 고른 기간. 기본 최근 7일 */
  initialPeriod?: AdminPeriod;
};

/**
 * a3 기능별 지표. 탭 8개(등록·소비·AI·식단표·장바구니·공유·리텐션·비용·오류)가 같은 틀을 쓴다.
 * 표가 하나인 탭은 표 옆에 막대 카드를, 표가 둘인 탭(AI)은 표 둘을 나란히 두고 막대 카드를 아래 전체 폭으로 둔다.
 * 상태: 기본 / 불러오는 중(a0-로딩) / 불러오기 실패(a0-불러오기 실패)
 */
export function MetricsContainer({
  tab,
  metrics,
  hasError = false,
  initialPeriod = DEFAULT_ADMIN_PERIOD,
}: MetricsContainerProps) {
  const { period, excludeSeed, setPeriod, setExcludeSeed } = useAdminPeriodQuery(initialPeriod);

  return (
    <>
      <AdminPageHeader
        title="기능별 지표"
        description="기능마다 품질과 사용량을 봐요. 이벤트 정의서 5장 M03~M20."
        actions={
          <AdminPeriodFilter
            period={period}
            onPeriodChange={setPeriod}
            excludeSeed={excludeSeed}
            onExcludeSeedChange={setExcludeSeed}
          />
        }
      />
      <MetricsTabs activeTab={tab} />
      {hasError ? (
        <AdminLoadError title="지표를 불러오지 못했어요" />
      ) : !metrics ? (
        <MetricsSkeleton hasCards={tab !== "ai"} />
      ) : (
        <MetricsContent metrics={metrics} />
      )}
    </>
  );
}

function MetricsContent({ metrics }: { metrics: MetricsTabData }) {
  const { cards, tables, daily } = metrics;
  // 표가 하나면 표와 막대 카드가 한 줄, 둘 이상이면 표들이 한 줄이고 막대 카드가 아래 전체 폭
  const isSplit = tables.length === 1;

  return (
    <div className={styles.content}>
      {cards.length > 0 && (
        <ul className={styles.cards}>
          {cards.map((card) => (
            <li key={card.name}>
              <MetricCard metric={card} />
            </li>
          ))}
        </ul>
      )}
      <div className={styles.row} data-split={isSplit || undefined}>
        {tables.map((table) => (
          <AdminTableCard
            key={table.key}
            title={table.title}
            meta={table.meta}
            columns={table.columns}
            rows={table.rows}
            note={table.note}
            relaxed={!isSplit}
            fillHeight={!isSplit}
          />
        ))}
        {isSplit && <MetricChartCard daily={daily} />}
      </div>
      {!isSplit && <MetricChartCard daily={daily} />}
    </div>
  );
}

/** a0-로딩: 지표 카드·표·막대 카드 자리에 스켈레톤 블록 */
function MetricsSkeleton({ hasCards }: { hasCards: boolean }) {
  return (
    <div className={styles.content} aria-busy="true">
      <span className={styles.srOnly} role="status">
        지표를 불러오는 중이에요
      </span>
      {hasCards && (
        <div className={styles.cards}>
          {Array.from({ length: 3 }, (_, index) => (
            <div key={index} className={styles.skeletonMetric}>
              <Skeleton width="9.375rem" height="1rem" />
              <Skeleton width="5rem" height="2.25rem" />
              <Skeleton width="8.75rem" height="0.75rem" />
            </div>
          ))}
        </div>
      )}
      <div className={styles.row} data-split={hasCards || undefined}>
        <AdminCard className={styles.skeletonCard}>
          <Skeleton width="9.375rem" height="1.125rem" />
          <Skeleton width="100%" height="12.5rem" />
        </AdminCard>
        <AdminCard className={styles.skeletonCard}>
          <Skeleton width="9.375rem" height="1.125rem" />
          <Skeleton width="100%" height="12.5rem" />
        </AdminCard>
      </div>
    </div>
  );
}
