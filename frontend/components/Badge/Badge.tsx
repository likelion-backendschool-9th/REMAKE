import type { ReactNode } from "react";

import { cx } from "@/lib/cx";

import styles from "./Badge.module.css";

export type BadgeTone = "danger" | "warning" | "safe" | "primary" | "neutral";

type BadgeProps = {
  /** 피그마 색: 빨강=danger, 주황=warning, 초록=safe, 파랑=primary, 회색=neutral */
  tone?: BadgeTone;
  children: ReactNode;
  className?: string;
};

/** 피그마 디자인 시스템 > 5. 칩·배지 > 배지. 작은 상태 표시(구독 중, 아침·점심·저녁 등). */
export function Badge({ tone = "neutral", children, className }: BadgeProps) {
  return (
    <span className={cx(styles.badge, className)} data-tone={tone}>
      {children}
    </span>
  );
}
