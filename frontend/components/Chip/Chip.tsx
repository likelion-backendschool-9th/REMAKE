import type { ButtonHTMLAttributes } from "react";

import { cx } from "@/lib/cx";

import styles from "./Chip.module.css";

type ChipProps = {
  isSelected: boolean;
  className?: string;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className">;

/** 피그마 디자인 시스템 > 5. 칩·배지 > 필터 칩. 목록 거르기·여러 개 고르기(재료 위치·종류, 알러지 선택). */
export function Chip({ isSelected, className, type = "button", ...rest }: ChipProps) {
  return (
    <button
      type={type}
      aria-pressed={isSelected}
      className={cx(styles.chip, className)}
      {...rest}
    />
  );
}
