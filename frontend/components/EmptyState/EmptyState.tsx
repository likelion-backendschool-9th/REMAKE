import type { ReactNode } from "react";

import { cx } from "@/lib/cx";

import styles from "./EmptyState.module.css";

/** 피그마 디자인 시스템 > 7. 목록 행·카드 > 빈 상태. 목록이 비었을 때 가운데 문구(장바구니·식단표·소비 패턴). */
export function EmptyState({ children, className }: { children: ReactNode; className?: string }) {
  return <p className={cx(styles.empty, className)}>{children}</p>;
}
