import { ArrowDownIcon, ArrowUpIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./RankChange.module.css";

type RankChangeProps = {
  direction: "up" | "down";
  /** 바뀐 순위 칸 수 */
  amount: number;
  className?: string;
};

/** 피그마 디자인 시스템 > 5. 칩·배지 > 순위 변동. 상승=파랑 ↑, 하락=빨강 ↓. */
export function RankChange({ direction, amount, className }: RankChangeProps) {
  const Icon = direction === "up" ? ArrowUpIcon : ArrowDownIcon;
  return (
    <span className={cx(styles.change, className)} data-direction={direction}>
      <Icon className={styles.icon} aria-hidden="true" />
      <span className={styles.amount} aria-hidden="true">
        {amount}
      </span>
      <span className={styles.srOnly}>
        {amount}계단 {direction === "up" ? "상승" : "하락"}
      </span>
    </span>
  );
}
