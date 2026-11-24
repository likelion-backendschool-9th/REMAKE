import type { ButtonHTMLAttributes } from "react";

import { cx } from "@/lib/cx";

import styles from "./Switch.module.css";

type SwitchProps = {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  className?: string;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children" | "onChange">;

/**
 * 피그마 디자인 시스템 > 3. 입력 > 토글. 상태: 켬/끔.
 * 이름은 aria-label 또는 옆 글자를 가리키는 aria-labelledby로 준다.
 */
export function Switch({ checked, onCheckedChange, className, onClick, ...rest }: SwitchProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      className={cx(styles.switch, className)}
      onClick={(event) => {
        onClick?.(event);
        if (!event.defaultPrevented) onCheckedChange(!checked);
      }}
      {...rest}
    >
      <span className={styles.knob} aria-hidden="true" />
    </button>
  );
}
