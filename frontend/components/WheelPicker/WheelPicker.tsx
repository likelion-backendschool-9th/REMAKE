"use client";

import { type KeyboardEvent, useEffect, useId, useRef } from "react";

import { cx } from "@/lib/cx";

import styles from "./WheelPicker.module.css";

export type WheelOption = { value: string; label: string };

export type WheelColumn = {
  /** 스크린 리더용 열 이름. 예: "연도", "월", "일" */
  label: string;
  options: readonly WheelOption[];
  value: string;
  onChange: (value: string) => void;
};

type WheelPickerProps = {
  /** 피그마 휠 3열(소비기한·시작일) / 휠 2열(캘린더 선택) */
  columns: readonly WheelColumn[];
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 입력 시트의 휠. 세 줄이 보이고 가운데 줄이 고른 값이다.
 * 스크롤·누르기·위아래 화살표 키로 고른다. 날짜 범위(윤년, 월별 일수)는 쓰는 쪽에서 options로 넘긴다.
 */
export function WheelPicker({ columns, className }: WheelPickerProps) {
  return (
    <div className={cx(styles.picker, className)}>
      <div className={styles.band} aria-hidden="true" />
      {columns.map((column) => (
        <Column key={column.label} column={column} />
      ))}
    </div>
  );
}

function Column({ column }: { column: WheelColumn }) {
  const { label, options, value, onChange } = column;
  const listRef = useRef<HTMLDivElement>(null);
  const idPrefix = useId();
  const timerRef = useRef<number | undefined>(undefined);
  const selectedIndex = Math.max(
    0,
    options.findIndex((o) => o.value === value),
  );

  const rowHeight = () =>
    listRef.current?.querySelector<HTMLElement>("[role=option]")?.offsetHeight ?? 40;

  // 값이 바뀌면 그 줄을 가운데로 맞춘다
  useEffect(() => {
    const list = listRef.current;
    if (!list) return;
    const top = selectedIndex * rowHeight();
    if (Math.abs(list.scrollTop - top) > 1) list.scrollTo({ top, behavior: "smooth" });
  }, [selectedIndex]);

  useEffect(() => () => window.clearTimeout(timerRef.current), []);

  // 스크롤이 멈추면 가운데 줄을 고른다
  const handleScroll = () => {
    window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => {
      const list = listRef.current;
      if (!list) return;
      const index = Math.min(
        options.length - 1,
        Math.max(0, Math.round(list.scrollTop / rowHeight())),
      );
      if (options[index] && options[index].value !== value) onChange(options[index].value);
    }, 120);
  };

  const handleKeyDown = (event: KeyboardEvent) => {
    const delta = { ArrowDown: 1, ArrowUp: -1 }[event.key];
    if (!delta) return;
    event.preventDefault();
    const next = options[selectedIndex + delta];
    if (next) onChange(next.value);
  };

  return (
    <div
      ref={listRef}
      className={styles.column}
      role="listbox"
      aria-label={label}
      aria-activedescendant={`${idPrefix}-${selectedIndex}`}
      tabIndex={0}
      onScroll={handleScroll}
      onKeyDown={handleKeyDown}
    >
      {options.map((option, index) => (
        <div
          key={option.value}
          id={`${idPrefix}-${index}`}
          role="option"
          aria-selected={index === selectedIndex}
          className={styles.option}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </div>
      ))}
    </div>
  );
}
