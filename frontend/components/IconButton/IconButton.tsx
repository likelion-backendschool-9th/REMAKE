import type { ButtonHTMLAttributes, ReactNode } from "react";
import Link from "next/link";

import { cx } from "@/lib/cx";

import styles from "./IconButton.module.css";

type IconButtonProps = {
  /** Solar 아이콘. 예: <CloseIcon /> (크기·색은 버튼이 맞춘다) */
  icon: ReactNode;
  /** 스크린 리더용 이름. 예: "닫기", "뒤로 가기" */
  label: string;
  /** 있으면 링크(이동), 없으면 버튼(동작)으로 그린다. */
  href?: string;
  className?: string;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children">;

/** 피그마 디자인 시스템 > 2. 버튼 > 아이콘 버튼. 44×44 터치 영역 안에 24 아이콘. 헤더·시트 닫기에 쓴다. */
export function IconButton({
  icon,
  label,
  href,
  className,
  type = "button",
  ...rest
}: IconButtonProps) {
  const iconNode = (
    <span className={styles.icon} aria-hidden="true">
      {icon}
    </span>
  );

  if (href) {
    return (
      <Link href={href} className={cx(styles.button, className)} aria-label={label}>
        {iconNode}
      </Link>
    );
  }

  return (
    <button type={type} className={cx(styles.button, className)} aria-label={label} {...rest}>
      {iconNode}
    </button>
  );
}
