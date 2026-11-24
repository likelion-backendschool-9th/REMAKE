"use client";

import { useId } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/Button/Button";
import { InputSheet } from "@/components/InputSheet/InputSheet";
import { TextField } from "@/components/TextField/TextField";

import styles from "./IngredientTextSheet.module.css";

type TextSheetKind = "name" | "price";

const nameSchema = z.object({ value: z.string().trim().min(1, "재료명을 작성해 주세요") });
// 가격은 선택 항목: 비우면 가격 없음
const priceSchema = z.object({ value: z.string().regex(/^\d*$/, "숫자만 작성해 주세요") });

const CONFIG = {
  name: {
    title: "재료명",
    schema: nameSchema,
    placeholder: "예: 만두",
    helperText: "제품명 대신 재료 이름으로 작성해 주세요",
    inputMode: "text",
    unit: undefined,
  },
  price: {
    title: "재료 가격",
    schema: priceSchema,
    placeholder: "숫자만 작성 가능합니다.",
    helperText: "가격을 정확히 작성할수록 소비 패턴 분석률이 올라가요",
    inputMode: "numeric",
    unit: "원",
  },
} as const;

type IngredientTextSheetProps = {
  kind: TextSheetKind;
  defaultValue: string;
  onClose: () => void;
  /** 확인: 재료명은 다듬은 글, 가격은 숫자 문자열(빈 값이면 가격 없음) */
  onConfirm: (value: string) => void;
};

/**
 * 재료 추가·수정의 글 입력 시트(피그마 입력 시트 > 텍스트). 재료명·재료 가격.
 * 열 때마다 처음 값으로 시작하도록 쓰는 쪽에서 열려 있을 때만 그린다.
 */
export function IngredientTextSheet({
  kind,
  defaultValue,
  onClose,
  onConfirm,
}: IngredientTextSheetProps) {
  const config = CONFIG[kind];
  const formId = useId();
  const { register, handleSubmit, control } = useForm<{ value: string }>({
    resolver: zodResolver(config.schema),
    defaultValues: { value: defaultValue },
  });
  const value = useWatch({ control, name: "value" });
  const error = config.schema.safeParse({ value }).error?.issues[0]?.message;
  // 재료명은 빈칸이면 버튼만 끄고 문구는 내지 않는다
  const shownError = kind === "name" && !value ? undefined : error;

  return (
    <InputSheet
      isOpen
      onClose={onClose}
      title={config.title}
      footer={
        <Button type="submit" form={formId} size="md" disabled={Boolean(error)}>
          확인
        </Button>
      }
    >
      <form
        id={formId}
        className={styles.form}
        onSubmit={handleSubmit(({ value: submitted }) => onConfirm(submitted.trim()))}
        noValidate
      >
        <TextField
          {...register("value")}
          aria-label={config.title}
          placeholder={config.placeholder}
          inputMode={config.inputMode}
          unit={config.unit}
          helperText={config.helperText}
          errorMessage={shownError}
          autoComplete="off"
          data-autofocus
        />
      </form>
    </InputSheet>
  );
}
