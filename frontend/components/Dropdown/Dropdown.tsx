import type { ButtonHTMLAttributes } from "react";

import { AltArrowDownIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./Dropdown.module.css";

type DropdownProps = {
  /** 피그마 크기: 작게(정렬) / 크게(기간) */
  size?: "sm" | "lg";
  className?: string;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className">;

/**
 * 피그마 디자인 시스템 > 4. 선택 > 드롭다운. 지금 고른 값과 ∨를 보여 주는 버튼.
 * 누르면 쓰는 화면이 선택 바텀시트(SelectSheet)를 띄운다.
 */
export function Dropdown({
  size = "sm",
  className,
  children,
  type = "button",
  ...rest
}: DropdownProps) {
  return (
    <button
      type={type}
      aria-haspopup="dialog"
      className={cx(styles.dropdown, className)}
      data-size={size}
      {...rest}
    >
      {children}
      <AltArrowDownIcon className={styles.icon} aria-hidden="true" />
    </button>
  );
}
