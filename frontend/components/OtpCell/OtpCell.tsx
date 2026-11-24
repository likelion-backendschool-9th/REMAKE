import type { InputHTMLAttributes, Ref } from "react";

import { cx } from "@/lib/cx";

import styles from "./OtpCell.module.css";

type OtpCellProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className" | "value"> & {
  /** 이 칸의 숫자 한 개. 빈칸이면 "" */
  value: string;
  /** 피그마 p1.3.3-계정 삭제-실패: 빨간 테두리. 오류 문구는 쓰는 화면이 칸 아래에 둔다 */
  isError?: boolean;
  /** 칸 사이 포커스 이동용 */
  ref?: Ref<HTMLInputElement>;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 3. 입력 > 인증번호 칸. 상태: 빈칸/포커스/입력됨/오류.
 * 6칸을 나란히 놓아 쓴다(계정 삭제 인증). 칸 사이 포커스 이동은 쓰는 화면에서 처리한다.
 */
export function OtpCell({ value, isError = false, className, ...inputProps }: OtpCellProps) {
  return (
    <input
      type="text"
      inputMode="numeric"
      autoComplete="one-time-code"
      maxLength={1}
      value={value}
      className={cx(styles.cell, className)}
      data-filled={value !== "" || undefined}
      data-error={isError || undefined}
      aria-invalid={isError || undefined}
      {...inputProps}
    />
  );
}
