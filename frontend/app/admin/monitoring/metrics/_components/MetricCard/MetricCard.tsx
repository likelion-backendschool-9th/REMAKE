import { Badge, type BadgeTone } from "@/components/Badge/Badge";

import type { MetricCardData, MetricStatus } from "../../_lib/metrics";

import styles from "./MetricCard.module.css";

const STATUS_BADGE: Record<MetricStatus, { tone: BadgeTone; label: string }> = {
  achieved: { tone: "safe", label: "달성" },
  trend: { tone: "neutral", label: "추세" },
  missed: { tone: "danger", label: "미달" },
};

/** a3 지표 카드: 이름과 목표 배지, 큰 값, 지표 번호와 목표 글. 값이 없으면 "-"를 보이고 배지는 숨긴다 */
export function MetricCard({ metric }: { metric: MetricCardData }) {
  const badge = metric.value !== null && metric.status ? STATUS_BADGE[metric.status] : null;
  return (
    <article className={styles.card}>
      <div className={styles.header}>
        <h2 className={styles.name}>{metric.name}</h2>
        {badge && <Badge tone={badge.tone}>{badge.label}</Badge>}
      </div>
      <p className={styles.value}>{metric.value ?? "-"}</p>
      <p className={styles.caption}>
        {metric.code} · {metric.caption}
      </p>
    </article>
  );
}
