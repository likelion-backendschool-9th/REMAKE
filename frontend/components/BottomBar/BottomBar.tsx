import type { ReactNode } from "react";

import { cx } from "@/lib/cx";

import styles from "./BottomBar.module.css";

type BottomBarProps = {
  /** 버튼 위 안내(예: "이번 달에 남은 식단표 생성 1회") */
  topNotice?: ReactNode;
  /** 버튼 아래 안내(예: "레시피 저장 시 부족한 재료는 장바구니로 들어가요") */
  bottomNotice?: ReactNode;
  /** 보통 <Button>. 약관 동의 줄처럼 버튼 위에 올 내용도 함께 넣는다. */
  children: ReactNode;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 6. 겹쳐 뜨는 것 > 하단 고정 영역. 화면 맨 아래 고정 버튼 묶음.
 * 종류: 버튼만 / 위 안내 / 아래 안내 / 약관 동의(children에 약관 줄 + 버튼).
 * 고정 영역에 내용이 가리지 않게 화면 쪽에 아래 여백을 둔다.
 */
export function BottomBar({ topNotice, bottomNotice, children, className }: BottomBarProps) {
  return (
    <div className={cx(styles.bar, className)}>
      {topNotice && <p className={styles.topNotice}>{topNotice}</p>}
      {children}
      {bottomNotice && <p className={styles.bottomNotice}>{bottomNotice}</p>}
    </div>
  );
}
