import { type InputHTMLAttributes, useId } from "react";

import { InfoCircleIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./TextField.module.css";

type TextFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, "className"> & {
  /** 입력칸 위 라벨. 없으면 aria-label을 꼭 넘긴다. */
  label?: string;
  /** 입력칸 아래 도움말 */
  helperText?: string;
  /** 있으면 오류 상태로 그리고 도움말 대신 이 문구를 보인다. */
  errorMessage?: string;
  /** 입력칸 오른쪽 단위. 예: "원" */
  unit?: string;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 3. 입력 > 입력 필드. 상태: 기본/포커스/입력됨/오류/비활성.
 * React Hook Form의 register() 결과를 그대로 펼쳐 넣을 수 있다(ref 포함).
 */
export function TextField({
  label,
  helperText,
  errorMessage,
  unit,
  className,
  id,
  ...inputProps
}: TextFieldProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const messageId = `${inputId}-message`;
  const message = errorMessage ?? helperText;
  const isError = Boolean(errorMessage);

  return (
    <div className={cx(styles.field, className)}>
      {label && (
        <label htmlFor={inputId} className={styles.label}>
          {label}
        </label>
      )}
      <div className={styles.box} data-error={isError || undefined}>
        <input
          id={inputId}
          className={styles.input}
          aria-invalid={isError || undefined}
          aria-describedby={message ? messageId : undefined}
          {...inputProps}
        />
        {unit && (
          <span className={styles.unit} aria-hidden="true">
            {unit}
          </span>
        )}
      </div>
      {message && (
        <p id={messageId} className={styles.message} data-error={isError || undefined}>
          <InfoCircleIcon className={styles.messageIcon} aria-hidden="true" />
          {message}
        </p>
      )}
    </div>
  );
}
