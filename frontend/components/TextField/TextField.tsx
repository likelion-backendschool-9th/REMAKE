import { type InputHTMLAttributes, useId } from "react";

import { InfoCircleIcon } from "@solar-icons/react/linear";

import { InputClearButton } from "@/components/InputClearButton/InputClearButton";
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
  /** 값이 있을 때 오른쪽 끝에 지우기(✕) 버튼을 보인다. 기본 true. 누르면 onChange("")가 불린다(register도 함께 비워짐) */
  clearable?: boolean;
  /** 지우기 버튼의 스크린 리더용 이름. 기본 "<label 또는 aria-label> 지우기". 라벨을 밖에 따로 둔 칸에 넘긴다 */
  clearLabel?: string;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 3. 입력 > 입력 필드. 상태: 기본/포커스/입력됨(지우기 버튼, 피그마 5991-7263)/오류/비활성.
 * React Hook Form의 register() 결과를 그대로 펼쳐 넣을 수 있다(ref 포함).
 */
export function TextField({
  label,
  helperText,
  errorMessage,
  unit,
  clearable = true,
  clearLabel,
  className,
  id,
  placeholder,
  ...inputProps
}: TextFieldProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const messageId = `${inputId}-message`;
  const message = errorMessage ?? helperText;
  const isError = Boolean(errorMessage);
  const showClear = clearable && !inputProps.disabled && !inputProps.readOnly;

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
          // 값이 없는지는 CSS(:placeholder-shown)로 본다. 안내 문구가 없어도 알 수 있게 빈칸 한 칸을 둔다
          placeholder={placeholder ?? " "}
          {...inputProps}
        />
        {unit && (
          <span className={styles.unit} aria-hidden="true">
            {unit}
          </span>
        )}
        {showClear && (
          <InputClearButton
            label={clearLabel ?? `${label ?? inputProps["aria-label"] ?? "입력 내용"} 지우기`}
            className={styles.clear}
          />
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
