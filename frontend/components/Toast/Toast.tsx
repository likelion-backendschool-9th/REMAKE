import type { ReactNode } from "react";

import { CheckIcon, InfoCircleIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./Toast.module.css";

type ToastProps = {
  /** 피그마 종류: 기본 / 성공(체크) / 오류(서버·인터넷·용량 등) */
  variant?: "default" | "success" | "error";
  /** true면 아래로 내려가며 사라지는 애니메이션. 다 사라진 뒤 지우는 건 쓰는 쪽(useTimedToast)이 한다 */
  isLeaving?: boolean;
  /** 사라지는 애니메이션이 끝났을 때. 여기서 토스트를 지운다(useTimedToast의 remove) */
  onLeft?: () => void;
  children: ReactNode;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 7. 목록 행·카드 > 토스트. 나타날 때 아래에서 위로 올라오고, isLeaving이면 아래로 내려가며 사라진다.
 * 화면 아래 위치·여러 개 쌓기는 토스트를 띄우는 쪽에서 정하고, 자동 닫힘 시간은 useTimedToast가 정한다.
 * 문구를 바꿔 끼우면 읽히도록 항상 그려져 있는 aria-live 영역 안에 넣는다(blueprint §6.3).
 */
export function Toast({
  variant = "default",
  isLeaving = false,
  onLeft,
  children,
  className,
}: ToastProps) {
  return (
    <div
      className={cx(styles.toast, className)}
      data-variant={variant}
      data-leaving={isLeaving || undefined}
      onAnimationEnd={() => {
        if (isLeaving) onLeft?.();
      }}
    >
      {variant === "success" && <CheckIcon className={styles.icon} aria-hidden="true" />}
      {variant === "error" && <InfoCircleIcon className={styles.icon} aria-hidden="true" />}
      <p className={styles.text}>{children}</p>
    </div>
  );
}
