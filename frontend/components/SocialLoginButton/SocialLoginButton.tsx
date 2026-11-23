import type { ButtonHTMLAttributes } from "react";
import Image from "next/image";

import { cx } from "@/lib/cx";

import styles from "./SocialLoginButton.module.css";

type Provider = "kakao" | "naver" | "google";

const PROVIDERS: Record<Provider, { label: string; width: number; height: number }> = {
  kakao: { label: "카카오로 시작하기", width: 18, height: 17 },
  naver: { label: "네이버로 시작하기", width: 18, height: 18 },
  google: { label: "Continue with Google", width: 18, height: 18 },
};

type SocialLoginButtonProps = {
  provider: Provider;
  className?: string;
} & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children">;

/** 피그마 디자인 시스템 > 1. 앱 틀 > 소셜 로그인 버튼. 글자색은 각 회사 가이드 색(변수 아님). */
export function SocialLoginButton({
  provider,
  className,
  type = "button",
  ...rest
}: SocialLoginButtonProps) {
  const { label, width, height } = PROVIDERS[provider];
  return (
    <button type={type} className={cx(styles.button, className)} data-provider={provider} {...rest}>
      <Image src={`/images/social/${provider}.svg`} alt="" width={width} height={height} />
      {label}
    </button>
  );
}
