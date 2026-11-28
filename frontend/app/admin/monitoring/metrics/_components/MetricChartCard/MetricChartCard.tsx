import { AdminCard } from "../../../../_components/AdminCard/AdminCard";
import { BarChart } from "../../../../_components/BarChart/BarChart";
import type { MetricDailyData } from "../../_lib/metrics";

import styles from "./MetricChartCard.module.css";

/** a3 막대 카드의 막대 폭·최대 높이(px). 대시보드(a1)의 32·96과 다르다 */
const BAR_WIDTH = 28;
const MAX_BAR_HEIGHT = 120;

type MetricChartCardProps = {
  daily: MetricDailyData;
  /** 표와 같은 줄에 놓을 때는 카드가 줄 높이만큼 늘어나고 그래프는 카드 아래에 붙는다 */
  className?: string;
};

/** a3 일별(리텐션은 주별) 막대 카드. `summary`가 있으면 그래프 오른쪽에 요약 숫자를 세로로 놓는다(AI 탭) */
export function MetricChartCard({ daily, className }: MetricChartCardProps) {
  return (
    <AdminCard title={daily.title} meta={daily.meta} className={className}>
      <div className={styles.body}>
        <div className={styles.chart}>
          <BarChart
            items={daily.items}
            aria-label={daily["aria-label"]}
            barWidth={BAR_WIDTH}
            maxBarHeight={MAX_BAR_HEIGHT}
          />
        </div>
        {daily.summary && (
          <dl className={styles.summary}>
            {daily.summary.map((item) => (
              <div key={item.label} className={styles.summaryItem}>
                <dt className={styles.summaryLabel}>{item.label}</dt>
                <dd className={styles.summaryValue}>{item.value}</dd>
              </div>
            ))}
          </dl>
        )}
      </div>
    </AdminCard>
  );
}
