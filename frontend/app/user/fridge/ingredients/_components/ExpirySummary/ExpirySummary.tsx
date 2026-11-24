"use client";

import type { ExpiryStatus } from "@/components/ExpiryLabel/ExpiryLabel";
import { Skeleton } from "@/components/Skeleton/Skeleton";

import { STATUS_LABEL, STATUS_ORDER } from "../../_lib/fridge";

import styles from "./ExpirySummary.module.css";

type ExpirySummaryProps = {
  counts: Record<ExpiryStatus, number>;
  /** 지금 거른 단계. 없으면 전체 */
  selected: ExpiryStatus | null;
  /** 누르면 그 단계로 거른다. 같은 단계를 다시 누르면 전체로 */
  onSelect: (status: ExpiryStatus | null) => void;
  isLoading?: boolean;
};

/** 피그마 디자인 시스템 > 요약 숫자 카드(ExpirySummary). 위험·임박·여유 개수. 누르면 그 단계로 거른다. */
export function ExpirySummary({
  counts,
  selected,
  onSelect,
  isLoading = false,
}: ExpirySummaryProps) {
  return (
    <div className={styles.card} role="group" aria-label="소비기한 단계별 개수">
      {STATUS_ORDER.map((status) => (
        <button
          key={status}
          type="button"
          className={styles.cell}
          aria-pressed={selected === status}
          onClick={() => onSelect(selected === status ? null : status)}
        >
          <span className={styles.label}>
            <span className={styles.dot} data-status={status} aria-hidden="true" />
            {STATUS_LABEL[status]}
          </span>
          {isLoading ? (
            <Skeleton width="0.8125rem" height="1.1875rem" radius="var(--radius-xs)" />
          ) : (
            <span className={styles.count}>{counts[status]}</span>
          )}
        </button>
      ))}
    </div>
  );
}
