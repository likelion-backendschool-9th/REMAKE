"use client";

import { type KeyboardEvent, useRef } from "react";

import { cx } from "@/lib/cx";

import styles from "./SegmentedControl.module.css";

type Option<T extends string> = { value: T; label: string };

type SegmentedControlProps<T extends string> = {
  options: readonly Option<T>[];
  value: T;
  onChange: (value: T) => void;
  /** 스크린 리더용 묶음 이름. 예: "재료 처리 방법" */
  label: string;
  className?: string;
};

/** 피그마 디자인 시스템 > 4. 선택 > 세그먼트. 둘 중 하나 고르기(재료 사용 시트: 사용 했어요/버렸어요). */
export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
  label,
  className,
}: SegmentedControlProps<T>) {
  const refs = useRef<Array<HTMLButtonElement | null>>([]);

  // 라디오 묶음 키보드 동작: 화살표로 옮기면서 고른다
  const handleKeyDown = (event: KeyboardEvent, index: number) => {
    const delta = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[event.key];
    if (!delta) return;
    event.preventDefault();
    const next = (index + delta + options.length) % options.length;
    onChange(options[next].value);
    refs.current[next]?.focus();
  };

  return (
    <div className={cx(styles.group, className)} role="radiogroup" aria-label={label}>
      {options.map((option, index) => {
        const isSelected = option.value === value;
        return (
          <button
            key={option.value}
            ref={(el) => {
              refs.current[index] = el;
            }}
            type="button"
            role="radio"
            aria-checked={isSelected}
            tabIndex={isSelected ? 0 : -1}
            className={styles.option}
            onClick={() => onChange(option.value)}
            onKeyDown={(event) => handleKeyDown(event, index)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
