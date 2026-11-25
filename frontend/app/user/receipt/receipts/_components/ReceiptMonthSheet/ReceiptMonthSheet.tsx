"use client";

import { useState } from "react";

import { Button } from "@/components/Button/Button";
import { InputSheet } from "@/components/InputSheet/InputSheet";
import { type WheelOption, WheelPicker } from "@/components/WheelPicker/WheelPicker";

type ReceiptMonthSheetProps = {
  /** 지금 고른 달 "2026-11" */
  month: string;
  /** 고를 수 있는 달(지난 6개월, recentMonths) */
  months: readonly string[];
  onClose: () => void;
  onConfirm: (month: string) => void;
};

/**
 * 피그마 p3-영수증-날짜 선택: "날짜 선택" 입력 시트, 연·월 휠 2열(26 / 11), "수정".
 * 영수증은 구매일로부터 6개월만 보관하므로(domain-rules 3) 휠에는 그 기간의 연·월만 둔다.
 */
export function ReceiptMonthSheet({ month, months, onClose, onConfirm }: ReceiptMonthSheetProps) {
  const [year, setYear] = useState(month.slice(0, 4));
  const [monthNumber, setMonthNumber] = useState(month.slice(5, 7));

  const years = [...new Set(months.map((value) => value.slice(0, 4)))].sort();
  const monthsOfYear = (target: string) =>
    months
      .filter((value) => value.startsWith(`${target}-`))
      .map((value) => value.slice(5, 7))
      .sort();

  const yearOptions: WheelOption[] = years.map((value) => ({ value, label: value.slice(-2) }));
  const monthOptions: WheelOption[] = monthsOfYear(year).map((value) => ({
    value,
    label: String(Number(value)),
  }));

  // 연도를 바꿔 그 해에 없는 월이 되면 가장 가까운 끝 달로 맞춘다
  const changeYear = (next: string) => {
    setYear(next);
    const available = monthsOfYear(next);
    if (!available.includes(monthNumber)) {
      setMonthNumber(
        next < year ? (available.at(-1) ?? monthNumber) : (available[0] ?? monthNumber),
      );
    }
  };

  return (
    <InputSheet
      isOpen
      onClose={onClose}
      title="날짜 선택"
      footer={
        <Button size="md" onClick={() => onConfirm(`${year}-${monthNumber}`)}>
          수정
        </Button>
      }
    >
      <WheelPicker
        columns={[
          { label: "연도", options: yearOptions, value: year, onChange: changeYear },
          { label: "월", options: monthOptions, value: monthNumber, onChange: setMonthNumber },
        ]}
      />
    </InputSheet>
  );
}
