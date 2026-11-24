import type { ReactNode } from "react";

import { InfoCircleIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./ResultState.module.css";

type ResultStateProps = {
  /** 피그마 종류: 오류(빨강 원)·안내(파랑 원)·로딩(돌아가는 호) */
  variant: "error" | "info" | "loading";
  title: string;
  description?: ReactNode;
  /** 아래에 둘 버튼(다시 시도 등) */
  action?: ReactNode;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 7. 목록 행·카드 > 결과 안내. 화면 가운데 결과 안내(OCR 실패, 생성 중, 횟수 소진 등).
 * 로딩은 role="status"로 알리고, 오류는 role="alert"로 바로 읽힌다.
 */
export function ResultState({ variant, title, description, action, className }: ResultStateProps) {
  return (
    <div
      className={cx(styles.result, className)}
      data-variant={variant}
      role={variant === "loading" ? "status" : variant === "error" ? "alert" : undefined}
    >
      <div className={styles.circle} aria-hidden="true">
        {variant === "loading" ? (
          <svg className={styles.spinner} viewBox="0 0 72 72" fill="none">
            <circle cx="36" cy="36" r="25" strokeWidth="6" className={styles.track} />
            <circle
              cx="36"
              cy="36"
              r="25"
              strokeWidth="6"
              strokeLinecap="round"
              pathLength="100"
              strokeDasharray="60 100"
              className={styles.arc}
            />
          </svg>
        ) : (
          <InfoCircleIcon className={styles.icon} />
        )}
      </div>
      <p className={styles.title}>{title}</p>
      {description && <p className={styles.description}>{description}</p>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
