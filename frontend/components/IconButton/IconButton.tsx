import type { ButtonHTMLAttributes, ReactNode } from "react";
import Link from "next/link";

import { cx } from "@/lib/cx";

import styles from "./IconButton.module.css";

type IconButtonProps = {
  /**
   * Solar 아이콘(색은 버튼이 맞춘다). 피그마 아이콘별 크기·선 굵기(선 굵기는 24 기준 단위):
   * 뒤로 24 <ArrowLeftIcon strokeWidth={2.85} />, 닫기 16 <CloseIcon strokeWidth={2.25} />,
   * 더보기 16 <MenuDotsVerticalIcon />(bold), 설정 24 <SettingsIcon />(outline),
   * 별 20 <StarIcon />(bold), 신고 24 <FlagIcon />(outline), 더하기 20 <AddIcon strokeWidth={1.8} />
   */
  icon: ReactNode;
  /** 아이콘 크기(px). 터치 영역은 항상 44×44. 기본 24 */
  iconSize?: 16 | 20 | 24;
  /** 스크린 리더용 이름. 예: "닫기", "뒤로 가기" */
  label: string;
  /** 있으면 링크(이동), 없으면 버튼(동작)으로 그린다. */
  href?: string;
  className?: string;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children">;

/** 피그마 디자인 시스템 > 2. 버튼 > 아이콘 버튼. 44×44 터치 영역 안에 아이콘(16·20·24). 헤더·시트 닫기에 쓴다. */
export function IconButton({
  icon,
  iconSize = 24,
  label,
  href,
  className,
  type = "button",
  ...rest
}: IconButtonProps) {
  const iconNode = (
    <span className={styles.icon} data-size={iconSize} aria-hidden="true">
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
