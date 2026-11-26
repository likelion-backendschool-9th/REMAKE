import type { CSSProperties } from "react";

import { formatNumber } from "@/lib/format";

import type { FunnelStep } from "../../_lib/funnel";

import styles from "./FunnelChart.module.css";

type FunnelChartProps = {
  steps: FunnelStep[];
};

/**
 * 퍼널 단계별 가로 막대(피그마 a2-퍼널). 칸: 단계(170) · 사용자 수 막대 · 사람 수(80) · 이전 단계 대비(140).
 * 막대 길이는 첫 단계 사람 수를 100%로 본다.
 * 운영자 화면 공통 차트(#284)가 정해지면 그쪽으로 옮기거나 바꾼다
 */
export function FunnelChart({ steps }: FunnelChartProps) {
  const firstUsers = steps[0]?.users ?? 0;

  return (
    <div className={styles.funnel}>
      <div className={styles.row} data-head="true" aria-hidden="true">
        <span>단계</span>
        <span className={styles.usersHead}>사용자 수</span>
        <span>이전 단계 대비</span>
      </div>
      <ol className={styles.steps}>
        {steps.map((step, index) => {
          const ratio = firstUsers > 0 ? step.users / firstUsers : 0;
          return (
            <li key={`${step.eventName}-${index}`} className={styles.row}>
              <div className={styles.step}>
                <span className={styles.stepLabel}>
                  {index + 1}. {step.label}
                </span>
                <span className={styles.eventName}>{step.eventName}</span>
              </div>
              <span className={styles.track} aria-hidden="true">
                <span className={styles.fill} style={{ "--fill-ratio": ratio } as CSSProperties} />
              </span>
              <span className={styles.users}>{formatNumber(step.users)}명</span>
              <span className={styles.rate} data-drop={step.isLargeDrop || undefined}>
                {step.conversionRate === null ? (
                  <>
                    <span aria-hidden="true">-</span>
                    <span className={styles.srOnly}>첫 단계</span>
                  </>
                ) : (
                  <>
                    <span className={styles.srOnly}>이전 단계 대비 </span>
                    {step.conversionRate}%{step.isLargeDrop && <span>이탈 큼</span>}
                  </>
                )}
              </span>
            </li>
          );
        })}
      </ol>
    </div>
  );
}
