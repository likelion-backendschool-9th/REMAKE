"use client";

import { useId } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/Button/Button";
import { InputSheet } from "@/components/InputSheet/InputSheet";
import { TextField } from "@/components/TextField/TextField";
import { INGREDIENT_NAME_MAX, PRICE_MAX_DIGITS } from "@/lib/ingredient-form";

import styles from "./IngredientTextSheet.module.css";

type TextSheetKind = "name" | "price";

/**
 * 화면별 시트 문구. 냉장고(p2.1·p2.2)는 기본 문구(안내 한 줄 + ⓘ), 영수증(p3.3.1·p3.3.2)은 제목·버튼이 다르고
 * 안내가 "•" 여러 줄이다. 입력 검사(글자 수·숫자만)는 같다.
 */
export type TextSheetCopy = {
  title?: string;
  placeholder?: string;
  confirmLabel?: string;
  /** 입력칸 아래 "•" 안내 줄. 있으면 기본 안내 한 줄 대신 보인다 */
  notes?: readonly string[];
};

export type TextSheetCopyByKind = Partial<Record<TextSheetKind, TextSheetCopy>>;

const nameSchema = z.object({
  value: z
    .string()
    .trim()
    .min(1, "재료명을 작성해 주세요")
    .max(INGREDIENT_NAME_MAX, `재료명은 ${INGREDIENT_NAME_MAX}자까지 작성할 수 있어요`),
});
// 가격은 선택 항목: 비우면 가격 없음
const priceSchema = z.object({
  value: z
    .string()
    .regex(/^\d*$/, "숫자만 작성해 주세요")
    .max(PRICE_MAX_DIGITS, `가격은 ${PRICE_MAX_DIGITS}자리까지 작성할 수 있어요`),
});

const CONFIG = {
  name: {
    title: "재료명",
    schema: nameSchema,
    placeholder: "예: 만두",
    helperText: "제품명 대신 재료 이름으로 작성해 주세요",
    inputMode: "text",
    maxLength: INGREDIENT_NAME_MAX,
    unit: undefined,
  },
  price: {
    title: "재료 가격",
    schema: priceSchema,
    placeholder: "숫자만 작성 가능합니다.",
    helperText: "가격을 정확히 작성할수록 소비 패턴 분석률이 올라가요",
    inputMode: "numeric",
    // 숫자 외 글자를 쳤을 때 오류를 보여 주려고 한 칸 여유를 둔다(자릿수는 스키마가 막음)
    maxLength: PRICE_MAX_DIGITS + 1,
    unit: "원",
  },
} as const;

type IngredientTextSheetProps = {
  kind: TextSheetKind;
  /** 화면별 문구. 없으면 냉장고 문구 */
  copy?: TextSheetCopy;
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
  copy,
  defaultValue,
  onClose,
  onConfirm,
}: IngredientTextSheetProps) {
  const config = CONFIG[kind];
  const formId = useId();
  const notesId = useId();
  const title = copy?.title ?? config.title;
  const notes = copy?.notes;
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
      title={title}
      footer={
        <Button type="submit" form={formId} size="md" disabled={Boolean(error)}>
          {copy?.confirmLabel ?? "확인"}
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
          aria-label={title}
          placeholder={copy?.placeholder ?? config.placeholder}
          inputMode={config.inputMode}
          maxLength={config.maxLength}
          unit={config.unit}
          helperText={notes ? undefined : config.helperText}
          // "•" 안내는 입력칸 아래 목록으로 따로 그리므로 설명으로 연결한다(오류가 있으면 오류가 설명)
          {...(notes && !shownError ? { "aria-describedby": notesId } : {})}
          errorMessage={shownError}
          autoComplete="off"
          data-autofocus
        />
        {notes && !shownError && (
          <ul id={notesId} className={styles.notes}>
            {notes.map((note) => (
              <li key={note}>{note}</li>
            ))}
          </ul>
        )}
      </form>
    </InputSheet>
  );
}
