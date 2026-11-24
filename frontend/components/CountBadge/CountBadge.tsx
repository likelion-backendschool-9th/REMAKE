import { cx } from "@/lib/cx";

import styles from "./CountBadge.module.css";

type CountBadgeProps = {
  count: number;
  /** 피그마 색: 빨강=danger(알림 띠 개수), 파랑=primary(금주의 레시피 순위) */
  tone?: "danger" | "primary";
  /** 스크린 리더용 문구. 있으면 숫자 대신 읽는다. 예: "안 읽은 알림 2개" */
  label?: string;
  className?: string;
};

/** 피그마 디자인 시스템 > 5. 칩·배지 > 숫자 배지. 높이 18, 최소 폭 18. */
export function CountBadge({ count, tone = "danger", label, className }: CountBadgeProps) {
  return (
    <span className={cx(styles.badge, className)} data-tone={tone}>
      <span aria-hidden={label ? true : undefined}>{count}</span>
      {label && <span className={styles.srOnly}>{label}</span>}
    </span>
  );
}
