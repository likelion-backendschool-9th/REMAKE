import type { InputHTMLAttributes } from "react";

import { cx } from "@/lib/cx";

import styles from "./Radio.module.css";

type RadioProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className" | "type"> & {
  /** 피그마 색: 기본=primary, 위험=danger(신고 시트) */
  tone?: "primary" | "danger";
  className?: string;
};

/** 피그마 디자인 시스템 > 4. 선택 > 라디오. 하나만 고르는 선택. 보통 OptionItem 안에서 쓴다. */
export function Radio({ tone = "primary", className, ...inputProps }: RadioProps) {
  return (
    <input type="radio" className={cx(styles.radio, className)} data-tone={tone} {...inputProps} />
  );
}
