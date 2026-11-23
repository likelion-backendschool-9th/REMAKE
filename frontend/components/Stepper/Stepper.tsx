import type { ReactNode } from "react";

import { AddIcon, MinusIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./Stepper.module.css";

type StepperProps = {
  value: number;
  onChange: (value: number) => void;
  /** 한 번 누를 때 바뀌는 양. 단위별 값은 정책(blueprint §10 수량 입력)을 따른다. */
  step?: number;
  min?: number;
  max?: number;
  /** 가운데 글자. 예: v => `${v}단` */
  formatValue?: (value: number) => ReactNode;
  /** 스크린 리더용 이름. 예: "수량" */
  label: string;
  className?: string;
};

/** 피그마 디자인 시스템 > 3. 입력 > 스테퍼. 최소값이면 빼기가 비활성. */
export function Stepper({
  value,
  onChange,
  step = 1,
  min = 0,
  max = Number.POSITIVE_INFINITY,
  formatValue = (v) => v,
  label,
  className,
}: StepperProps) {
  // 0.5 단위 등 소수 계산 오차를 없앤다
  const round = (n: number) => Math.round(n * 1000) / 1000;
  const canDecrease = value > min;
  const canIncrease = value < max;

  return (
    <div className={cx(styles.stepper, className)} role="group" aria-label={label}>
      <button
        type="button"
        className={styles.control}
        onClick={() => onChange(Math.max(min, round(value - step)))}
        disabled={!canDecrease}
        aria-label={`${label} 줄이기`}
      >
        <MinusIcon aria-hidden="true" />
      </button>
      <output className={styles.value} aria-live="polite">
        {formatValue(value)}
      </output>
      <button
        type="button"
        className={styles.control}
        onClick={() => onChange(Math.min(max, round(value + step)))}
        disabled={!canIncrease}
        aria-label={`${label} 늘리기`}
      >
        <AddIcon aria-hidden="true" />
      </button>
    </div>
  );
}
