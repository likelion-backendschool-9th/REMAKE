import type { ReactNode } from "react";

import { InfoCircleIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./InfoBox.module.css";

type InfoBoxProps = {
  /** 피그마 종류: 회색=gray(커스텀 레시피 안내), 팁=tip(레시피 Tips), 한 줄=inline(입력 아래 ⓘ 안내) */
  variant?: "gray" | "tip" | "inline";
  children: ReactNode;
  className?: string;
};

/** 피그마 디자인 시스템 > 7. 목록 행·카드 > 안내 박스. */
export function InfoBox({ variant = "gray", children, className }: InfoBoxProps) {
  return (
    <div className={cx(styles.box, className)} data-variant={variant}>
      {variant === "tip" ? (
        <p className={styles.tipTitle}>Tips!</p>
      ) : (
        <InfoCircleIcon className={styles.icon} aria-hidden="true" />
      )}
      <div className={styles.text}>{children}</div>
    </div>
  );
}
