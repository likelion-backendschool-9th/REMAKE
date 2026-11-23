import type { ButtonHTMLAttributes } from "react";

import { cx } from "@/lib/cx";

import styles from "./Tab.module.css";

type TabProps = {
  isSelected: boolean;
  className?: string;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className" | "role">;

/**
 * 피그마 디자인 시스템 > 4. 선택 > 밑줄 탭(소비 패턴 월 탭). 선택=굵은 글자 + 파란 밑줄.
 * 쓰는 화면에서 role="tablist" 묶음으로 감싸고, 각 탭의 aria-controls를 패널 id에 연결한다.
 */
export function Tab({ isSelected, className, type = "button", ...rest }: TabProps) {
  return (
    <button
      type={type}
      role="tab"
      aria-selected={isSelected}
      tabIndex={isSelected ? 0 : -1}
      className={cx(styles.tab, className)}
      {...rest}
    />
  );
}
