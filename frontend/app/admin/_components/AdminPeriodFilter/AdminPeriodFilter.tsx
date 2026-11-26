"use client";

import { Chip } from "@/components/Chip/Chip";

import { ADMIN_PERIODS, type AdminPeriod } from "../../_lib/period";

import styles from "./AdminPeriodFilter.module.css";

type AdminPeriodFilterProps = {
  period: AdminPeriod;
  onPeriodChange: (period: AdminPeriod) => void;
  /** "실제 데이터만": 켜면 시드(시연용) 이벤트를 빼고 센다 */
  isRealOnly: boolean;
  onRealOnlyChange: (isRealOnly: boolean) => void;
};

/** 운영자 지표 화면의 기간 칩(오늘·최근 7일·최근 30일, 하나만 고름)과 "실제 데이터만" 켜고 끄기 */
export function AdminPeriodFilter({
  period,
  onPeriodChange,
  isRealOnly,
  onRealOnlyChange,
}: AdminPeriodFilterProps) {
  return (
    <div className={styles.filter}>
      <div className={styles.group} role="group" aria-label="기간">
        {ADMIN_PERIODS.map((item) => (
          <Chip
            key={item.value}
            isSelected={item.value === period}
            onClick={() => onPeriodChange(item.value)}
          >
            {item.label}
          </Chip>
        ))}
      </div>
      <Chip isSelected={isRealOnly} onClick={() => onRealOnlyChange(!isRealOnly)}>
        실제 데이터만
      </Chip>
    </div>
  );
}
