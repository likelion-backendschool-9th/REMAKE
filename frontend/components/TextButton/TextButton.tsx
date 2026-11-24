import type { ButtonHTMLAttributes, ReactNode } from "react";
import Link from "next/link";

import { AltArrowRightIcon } from "@solar-icons/react/linear";

import { cx } from "@/lib/cx";

import styles from "./TextButton.module.css";

type TextButtonProps = {
  /** 피그마 색: 파랑=primary, 회색=neutral, 빨강=danger */
  tone?: "primary" | "neutral" | "danger";
  /** 피그마 화살표=있음. 글자 오른쪽에 > 를 붙인다. */
  withArrow?: boolean;
  /** 있으면 링크(이동), 없으면 버튼(동작)으로 그린다. */
  href?: string;
  className?: string;
  children: ReactNode;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children">;

/** 피그마 디자인 시스템 > 2. 버튼 > 텍스트 버튼. 전체 보기·재전송 하기·자세히 보기 등. */
export function TextButton({
  tone = "primary",
  withArrow = false,
  href,
  className,
  children,
  type = "button",
  ...rest
}: TextButtonProps) {
  const content = (
    <>
      {children}
      {withArrow && <AltArrowRightIcon className={styles.arrow} aria-hidden="true" />}
    </>
  );

  if (href) {
    return (
      <Link href={href} className={cx(styles.button, className)} data-tone={tone}>
        {content}
      </Link>
    );
  }

  return (
    <button type={type} className={cx(styles.button, className)} data-tone={tone} {...rest}>
      {content}
    </button>
  );
}
