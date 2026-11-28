import Link from "next/link";

import { ADMIN_PATHS, type MetricsTab } from "../../../../_lib/admin-menu";
import { METRICS_TABS } from "../../_lib/metrics";

import styles from "./MetricsTabs.module.css";

/**
 * a3 기능별 지표 탭 8개. 탭마다 주소가 달라서 화면을 옮기는 링크(nav + aria-current)로 그린다.
 * 모양은 디자인 시스템 밑줄 탭(components/Tab)과 같다
 */
export function MetricsTabs({ activeTab }: { activeTab: MetricsTab }) {
  return (
    <nav className={styles.tabs} aria-label="기능별 지표 탭">
      {METRICS_TABS.map((item) => (
        <Link
          key={item.tab}
          href={ADMIN_PATHS.metrics(item.tab)}
          className={styles.tab}
          aria-current={item.tab === activeTab ? "page" : undefined}
        >
          {item.label}
        </Link>
      ))}
    </nav>
  );
}
