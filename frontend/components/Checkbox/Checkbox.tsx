import type { InputHTMLAttributes } from "react";

import { CheckIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./Checkbox.module.css";

type CheckboxProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className" | "type"> & {
  className?: string;
};

/** 피그마 디자인 시스템 > 4. 선택 > 체크박스. 여러 개 고르는 선택(장바구니, 재료 선택, 넣을 끼니). */
export function Checkbox({ className, ...inputProps }: CheckboxProps) {
  return (
    <span className={cx(styles.box, className)}>
      <input type="checkbox" className={styles.input} {...inputProps} />
      <CheckIcon className={styles.check} aria-hidden="true" />
    </span>
  );
}
