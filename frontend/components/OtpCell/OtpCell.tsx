import type { InputHTMLAttributes } from "react";

import { cx } from "@/lib/cx";

import styles from "./OtpCell.module.css";

type OtpCellProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className" | "value"> & {
  /** 이 칸의 숫자 한 개. 빈칸이면 "" */
  value: string;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 3. 입력 > 인증번호 칸. 상태: 빈칸/포커스/입력됨.
 * 6칸을 나란히 놓아 쓴다(계정 삭제 인증). 칸 사이 포커스 이동은 쓰는 화면에서 처리한다.
 */
export function OtpCell({ value, className, ...inputProps }: OtpCellProps) {
  return (
    <input
      type="text"
      inputMode="numeric"
      autoComplete="one-time-code"
      maxLength={1}
      value={value}
      className={cx(styles.cell, className)}
      data-filled={value !== "" || undefined}
      {...inputProps}
    />
  );
}
