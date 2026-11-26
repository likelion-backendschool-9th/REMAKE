import type { CSSProperties } from "react";

import styles from "./BarChart.module.css";

export type BarChartItem = {
  /** 막대 아래 글(예: "11/24") */
  label: string;
  value: number;
};

type BarChartProps = {
  items: BarChartItem[];
  /** 스크린 리더용 그래프 이름. 예: "주별 소비기한 내 소진율" */
  "aria-label": string;
  /** 막대 위에 보일 값 글. 마지막(강조) 막대 위에만 보인다. 없으면 숫자 그대로 */
  formatValue?: (value: number) => string;
};

/**
 * 세로 막대그래프(피그마 a1-대시보드 북극성 지표). 막대 폭 32, 가장 큰 값이 높이 96.
 * 마지막 막대(이번 주)만 진한 파랑에 값을 보이고, 나머지는 30% 투명도.
 * 운영자 화면 공통 차트(#284)가 정해지면 그쪽으로 옮기거나 바꾼다
 */
export function BarChart({ items, formatValue = String, ...rest }: BarChartProps) {
  const max = Math.max(...items.map((item) => item.value), 0);
  const summary = items.map((item) => `${item.label} ${formatValue(item.value)}`).join(", ");

  return (
    <div className={styles.chart} role="img" aria-label={`${rest["aria-label"]}: ${summary}`}>
      {items.map((item, index) => {
        const isCurrent = index === items.length - 1;
        const ratio = max > 0 ? item.value / max : 0;
        return (
          <div key={item.label} className={styles.column} aria-hidden="true">
            {isCurrent && <span className={styles.value}>{formatValue(item.value)}</span>}
            <span
              className={styles.bar}
              data-current={isCurrent || undefined}
              style={{ "--bar-ratio": ratio } as CSSProperties}
            />
            <span className={styles.label}>{item.label}</span>
          </div>
        );
      })}
    </div>
  );
}
