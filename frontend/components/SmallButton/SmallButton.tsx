import type { ButtonHTMLAttributes } from "react";

import { cx } from "@/lib/cx";

import styles from "./SmallButton.module.css";

type SmallButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  /** 피그마 종류: 파랑=primary(수량 변경·첨부 하기), 회색=neutral(선택 삭제), 테두리=outline */
  tone?: "primary" | "neutral" | "outline";
};

/** 피그마 디자인 시스템 > 2. 버튼 > 작은 버튼. 리스트 오른쪽 작은 버튼. */
export function SmallButton({
  tone = "primary",
  type = "button",
  className,
  ...rest
}: SmallButtonProps) {
  return <button type={type} className={cx(styles.button, className)} data-tone={tone} {...rest} />;
}
