"use client";

import { useState } from "react";

import { Button } from "@/components/Button/Button";
import { InputSheet } from "@/components/InputSheet/InputSheet";
import { type WheelOption, WheelPicker } from "@/components/WheelPicker/WheelPicker";
import { dayjs } from "@/lib/dayjs";

/** 고를 수 있는 연도: 올해부터 5년 뒤까지 */
const YEAR_SPAN = 5;

function range(from: number, to: number, suffix: string): WheelOption[] {
  return Array.from({ length: to - from + 1 }, (_, i) => {
    const n = from + i;
    return { value: String(n), label: `${n}${suffix}` };
  });
}

type ExpiryDateSheetProps = {
  /** "2026-12-21". 없으면 오늘에서 시작 */
  defaultValue: string | null;
  onClose: () => void;
  onConfirm: (date: string) => void;
};

/**
 * 재료 추가·수정의 소비 기한 시트(피그마 입력 시트 > 휠 3열: 연·월·일).
 * 달마다 일수(윤년 포함)를 맞춘다. 열려 있을 때만 그린다.
 */
export function ExpiryDateSheet({ defaultValue, onClose, onConfirm }: ExpiryDateSheetProps) {
  const [date, setDate] = useState(() => {
    const start = defaultValue ? dayjs(defaultValue) : dayjs();
    return {
      year: String(start.year()),
      month: String(start.month() + 1),
      day: String(start.date()),
    };
  });
  const [thisYear] = useState(() => dayjs().year());

  const firstYear = Math.min(thisYear, Number(date.year));
  const daysInMonth = dayjs(`${date.year}-${date.month.padStart(2, "0")}-01`).daysInMonth();

  const handleConfirm = () => {
    const value = dayjs(
      `${date.year}-${date.month.padStart(2, "0")}-${date.day.padStart(2, "0")}`,
    ).format("YYYY-MM-DD");
    onConfirm(value);
  };

  return (
    <InputSheet
      isOpen
      onClose={onClose}
      title="소비 기한"
      footer={
        <Button size="md" onClick={handleConfirm}>
          확인
        </Button>
      }
    >
      <WheelPicker
        columns={[
          {
            label: "연도",
            options: range(firstYear, thisYear + YEAR_SPAN, "년"),
            value: date.year,
            onChange: (year) => setDate((prev) => ({ ...prev, year })),
          },
          {
            label: "월",
            options: range(1, 12, "월"),
            value: date.month,
            onChange: (month) => setDate((prev) => ({ ...prev, month })),
          },
          {
            label: "일",
            options: range(1, daysInMonth, "일"),
            value: date.day,
            onChange: (day) => setDate((prev) => ({ ...prev, day })),
          },
        ]}
      />
    </InputSheet>
  );
}
