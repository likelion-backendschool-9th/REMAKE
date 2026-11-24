import type { InputHTMLAttributes } from "react";

import { MagnifierIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./SearchField.module.css";

type SearchFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className" | "type"> & {
  /** 보이는 라벨이 없으므로 스크린 리더용 이름을 꼭 넘긴다. 예: "레시피 검색" */
  "aria-label": string;
  className?: string;
};

/** 피그마 디자인 시스템 > 3. 입력 > 검색창. 상태: 기본/포커스. */
export function SearchField({ className, ...inputProps }: SearchFieldProps) {
  return (
    <div className={cx(styles.box, className)}>
      <MagnifierIcon className={styles.icon} aria-hidden="true" />
      <input type="search" className={styles.input} {...inputProps} />
    </div>
  );
}
