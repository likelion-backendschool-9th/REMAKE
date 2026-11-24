"use client";

import type { UseFormRegisterReturn } from "react-hook-form";

import { TextField } from "@/components/TextField/TextField";

import { getHouseholdError, HOUSEHOLD_MAX } from "../../_lib/household";

type HouseholdSizeFieldProps = {
  /** register("householdSize") 결과 */
  field: UseFormRegisterReturn<"householdSize">;
  /** 지금 입력값(useWatch). 단위·오류 표시에 쓴다 */
  value: string;
  placeholder: string;
  helperText?: string;
};

/**
 * "가구원 수" 입력칸. 숫자만 받고(다른 글자는 즉시 지움) 1~50이 아니면 오류 문구, 값이 있으면 단위 "명".
 * 온보딩 p1.1과 설정 p1.3.1이 쓴다.
 */
export function HouseholdSizeField({
  field,
  value,
  placeholder,
  helperText,
}: HouseholdSizeFieldProps) {
  return (
    <TextField
      label="가구원 수"
      placeholder={placeholder}
      inputMode="numeric"
      autoComplete="off"
      maxLength={String(HOUSEHOLD_MAX).length}
      unit={value ? "명" : undefined}
      helperText={helperText}
      errorMessage={getHouseholdError(value)}
      {...field}
      onChange={(event) => {
        // 숫자가 아닌 글자는 입력하는 즉시 지운다(정책: 숫자만)
        event.target.value = event.target.value.replace(/\D/g, "");
        return field.onChange(event);
      }}
    />
  );
}
