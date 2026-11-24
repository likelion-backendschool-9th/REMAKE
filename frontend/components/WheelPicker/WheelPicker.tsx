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
 * options가 바뀌어 지금 value가 없어지면(1월 31일 → 2월) 가장 가까운 값으로 onChange를 불러 맞춘다.
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

/** value가 options에 없을 때 대신 고를 값. 숫자면 가장 가까운 값(31일 → 28일), 아니면 첫 값 */
function nearestOption(options: readonly WheelOption[], value: string): WheelOption | undefined {
  const target = Number(value);
  if (!Number.isFinite(target)) return options[0];
  let nearest = options[0];
  for (const option of options) {
    const distance = Math.abs(Number(option.value) - target);
    if (Number.isFinite(distance) && distance < Math.abs(Number(nearest?.value) - target)) {
      nearest = option;
    }
  }
  return nearest;
}

function Column({ column }: { column: WheelColumn }) {
  const { label, options, value, onChange } = column;
  const listRef = useRef<HTMLDivElement>(null);
  const idPrefix = useId();
  const timerRef = useRef<number | undefined>(undefined);
  const valueIndex = options.findIndex((o) => o.value === value);
  // 고른 값이 새 options에 없으면(1월 31일 → 2월) 가까운 값을 고른 줄로 보이고, 값도 그것으로 바꾼다
  const fallback = valueIndex === -1 ? nearestOption(options, value) : undefined;
  const selectedIndex = fallback ? options.indexOf(fallback) : Math.max(0, valueIndex);

  useEffect(() => {
    if (fallback) onChange(fallback.value);
  }, [fallback, onChange]);

  const rowHeight = () =>
    listRef.current?.querySelector<HTMLElement>("[role=option]")?.offsetHeight ?? 40;

  const hasPositionedRef = useRef(false);

  // 값이 바뀌면 그 줄을 가운데로 맞춘다. 처음 열 때는 바로 옮긴다:
  // 먼 줄까지 부드럽게 굴리면 중간에 멈춘 자리를 스크롤 처리기가 고른 값으로 읽어 값이 바뀔 수 있다.
  useEffect(() => {
    const list = listRef.current;
    if (!list) return;
    const top = selectedIndex * rowHeight();
    const behavior = hasPositionedRef.current ? "smooth" : "instant";
    hasPositionedRef.current = true;
    if (Math.abs(list.scrollTop - top) > 1) list.scrollTo({ top, behavior });
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
