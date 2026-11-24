import { cx } from "@/lib/cx";

import styles from "./ExpiryLabel.module.css";

/** 서버가 주는 소비기한 상태(domain-rules: DANGER=지남·D-DAY, SOON=D-1~D-3, OK=그 외) */
export type ExpiryStatus = "DANGER" | "SOON" | "OK";

type ExpiryLabelProps = {
  status: ExpiryStatus;
  /** 서버가 주는 dDay 문구 그대로(예: "D-3"). 기기 시계로 다시 계산하지 않는다. */
  text: string;
  /** 피그마 모양: 글자=text(재료 목록 오른쪽), 배지=badge(재료 사용 시트) */
  variant?: "text" | "badge";
  className?: string;
};

/** 피그마 디자인 시스템 > 5. 칩·배지 > 소비기한. 위험=빨강, 임박=주황, 여유=초록. */
export function ExpiryLabel({ status, text, variant = "text", className }: ExpiryLabelProps) {
  return (
    <span className={cx(styles.label, className)} data-status={status} data-variant={variant}>
      {text}
    </span>
  );
}
