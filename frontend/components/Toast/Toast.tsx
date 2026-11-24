import type { ReactNode } from "react";

import { CheckIcon, InfoCircleIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./Toast.module.css";

type ToastProps = {
  /** 피그마 종류: 기본 / 성공(체크) / 오류(서버·인터넷·용량 등) */
  variant?: "default" | "success" | "error";
  children: ReactNode;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 7. 목록 행·카드 > 토스트. 모양만 그린다.
 * 화면 아래 위치·자동 닫힘·여러 개 쌓기는 토스트를 띄우는 쪽(공통 토스트 영역)에서 정한다.
 * 문구를 바꿔 끼우면 읽히도록 항상 그려져 있는 aria-live 영역 안에 넣는다(blueprint §6.3).
 */
export function Toast({ variant = "default", children, className }: ToastProps) {
  return (
    <div className={cx(styles.toast, className)} data-variant={variant}>
      {variant === "success" && <CheckIcon className={styles.icon} aria-hidden="true" />}
      {variant === "error" && <InfoCircleIcon className={styles.icon} aria-hidden="true" />}
      <p className={styles.text}>{children}</p>
    </div>
  );
}
