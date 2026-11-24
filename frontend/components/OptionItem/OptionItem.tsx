import type { InputHTMLAttributes, ReactNode } from "react";

import { Checkbox } from "@/components/Checkbox/Checkbox";
import { Radio } from "@/components/Radio/Radio";
import { cx } from "@/lib/cx";

import styles from "./OptionItem.module.css";

type OptionItemProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className" | "type"> & {
  /** 피그마 종류: 라디오(하나 고르기) / 체크박스(여러 개 고르기) */
  type?: "radio" | "checkbox";
  /** 피그마 색: 기본/위험(라디오만) */
  tone?: "primary" | "danger";
  children: ReactNode;
  className?: string;
};

/** 피그마 디자인 시스템 > 4. 선택 > 선택 항목. 바텀시트 안의 한 줄. 선택 안 된 줄은 글자가 흐리다. */
export function OptionItem({
  type = "radio",
  tone = "primary",
  children,
  className,
  ...inputProps
}: OptionItemProps) {
  return (
    <label className={cx(styles.item, className)}>
      {type === "radio" ? <Radio tone={tone} {...inputProps} /> : <Checkbox {...inputProps} />}
      <span className={styles.text}>{children}</span>
    </label>
  );
}
