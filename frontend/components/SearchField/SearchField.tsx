import type { InputHTMLAttributes } from "react";

import { MagnifierIcon } from "@solar-icons/react/linear";

import { InputClearButton } from "@/components/InputClearButton/InputClearButton";
import { cx } from "@/lib/cx";

import styles from "./SearchField.module.css";

type SearchFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className" | "type"> & {
  /** 보이는 라벨이 없으므로 스크린 리더용 이름을 꼭 넘긴다. 예: "레시피 검색" */
  "aria-label": string;
  /** 값이 있을 때 오른쪽에 지우기(✕) 버튼을 보인다. 기본 true. 누르면 onChange("")가 불린다 */
  clearable?: boolean;
  className?: string;
};

/** 피그마 디자인 시스템 > 3. 입력 > 검색창. 상태: 기본/포커스/값 있음(지우기 버튼, 피그마 5780-4568). */
export function SearchField({
  className,
  clearable = true,
  placeholder,
  ...inputProps
}: SearchFieldProps) {
  const showClear = clearable && !inputProps.disabled && !inputProps.readOnly;

  return (
    <div className={cx(styles.box, className)}>
      <MagnifierIcon className={styles.icon} aria-hidden="true" />
      {/* 값이 없는지는 CSS(:placeholder-shown)로 본다. 안내 문구가 없어도 알 수 있게 빈칸 한 칸을 둔다 */}
      <input
        type="search"
        className={styles.input}
        placeholder={placeholder ?? " "}
        {...inputProps}
      />
      {showClear && <InputClearButton label="검색어 지우기" className={styles.clear} />}
    </div>
  );
}
