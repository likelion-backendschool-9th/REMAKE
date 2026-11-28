"use client";

import { useId, useState } from "react";

import { BottomSheet } from "@/components/BottomSheet/BottomSheet";
import { Button } from "@/components/Button/Button";
import { OptionItem } from "@/components/OptionItem/OptionItem";

import styles from "./SelectSheet.module.css";

/** disabled: 지금 고를 수 없는 항목(예: 이번 주에 이미 공유한 레시피의 "공유하기") */
type Option<T extends string> = { value: T; label: string; disabled?: boolean };

type SelectSheetProps<T extends string> = {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  options: readonly Option<T>[];
  /** 열 때 미리 골라 둘 값. 단일 선택이면 0~1개 */
  defaultValues: readonly T[];
  /** 확인을 누르면 고른 값을 넘긴다. 시트는 닫지 않으므로 쓰는 화면에서 닫는다. */
  onConfirm: (values: T[]) => void;
  /** 피그마 종류: 단일=라디오, 복수=체크박스 */
  multiple?: boolean;
  /** 피그마 종류=위험(신고 시트). 위험이면 아무것도 안 골라도 버튼이 빨간색 그대로다(누르면 아무 일 없음) */
  tone?: "primary" | "danger";
  confirmLabel?: string;
  /** 항목 아래 안내 문구 */
  notice?: string;
};

/**
 * 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 바텀시트/선택.
 * 단일(재료 위치·종류·식단 목적·커스텀 옵션), 복수(넣을 끼니), 위험(신고).
 */
export function SelectSheet<T extends string>({
  isOpen,
  onClose,
  title,
  options,
  defaultValues,
  onConfirm,
  multiple = false,
  tone = "primary",
  confirmLabel = "확인",
  notice,
}: SelectSheetProps<T>) {
  return (
    <BottomSheet isOpen={isOpen} onClose={onClose} title={title}>
      {/* 열 때마다 고른 값을 처음 상태로 되돌리려고 isOpen일 때만 그린다 */}
      {isOpen && (
        <SelectSheetBody
          options={options}
          defaultValues={defaultValues}
          onConfirm={onConfirm}
          multiple={multiple}
          tone={tone}
          confirmLabel={confirmLabel}
          notice={notice}
          title={title}
        />
      )}
    </BottomSheet>
  );
}

function SelectSheetBody<T extends string>({
  options,
  defaultValues,
  onConfirm,
  multiple,
  tone,
  confirmLabel,
  notice,
  title,
}: Pick<SelectSheetProps<T>, "options" | "defaultValues" | "onConfirm" | "notice" | "title"> & {
  multiple: boolean;
  tone: "primary" | "danger";
  confirmLabel: string;
}) {
  const [selected, setSelected] = useState<T[]>(() => [...defaultValues]);
  const name = useId();
  const isEmpty = selected.length === 0;
  // 피그마 신고 시트(위험)는 아무것도 안 골라도 버튼이 빨갛다. 누를 수 없다는 건 aria-disabled로만 알린다
  const keepsColorWhenEmpty = tone === "danger";

  const toggle = (value: T) => {
    setSelected((prev) => {
      if (!multiple) return [value];
      return prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value];
    });
  };

  return (
    <>
      <fieldset className={styles.options}>
        <legend className={styles.srOnly}>{title}</legend>
        {options.map((option) => (
          <OptionItem
            key={option.value}
            type={multiple ? "checkbox" : "radio"}
            tone={tone}
            name={name}
            value={option.value}
            checked={selected.includes(option.value)}
            disabled={option.disabled}
            onChange={() => toggle(option.value)}
          >
            {option.label}
          </OptionItem>
        ))}
      </fieldset>
      {notice && <p className={styles.notice}>{notice}</p>}
      <Button
        tone={tone}
        className={styles.confirm}
        disabled={isEmpty && !keepsColorWhenEmpty}
        aria-disabled={(isEmpty && keepsColorWhenEmpty) || undefined}
        onClick={() => {
          if (!isEmpty) onConfirm(selected);
        }}
      >
        {confirmLabel}
      </Button>
    </>
  );
}
