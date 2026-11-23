import type { ButtonHTMLAttributes } from "react";

import { cx } from "@/lib/cx";

import styles from "./Button.module.css";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  /** 피그마 색: 기본=primary, 위험=danger(계정 삭제·신고·구독 해지) */
  tone?: "primary" | "danger";
  /** 피그마 상태=로딩. 점 세 개를 보여 주고 누를 수 없게 한다. */
  isLoading?: boolean;
};

/** 피그마 디자인 시스템 > 2. 버튼 > 버튼. 화면 아래 큰 버튼. 폭은 부모에 맞춰 늘어난다. */
export function Button({
  tone = "primary",
  isLoading = false,
  disabled,
  type = "button",
  className,
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cx(styles.button, className)}
      data-tone={tone}
      data-loading={isLoading || undefined}
      disabled={disabled || isLoading}
      aria-busy={isLoading || undefined}
      {...rest}
    >
      <span className={styles.label}>{children}</span>
      {isLoading && (
        <span className={styles.dots} aria-hidden="true">
          <span />
          <span />
          <span />
        </span>
      )}
    </button>
  );
}
