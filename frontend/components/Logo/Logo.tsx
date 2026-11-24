import Image from "next/image";

import { cx } from "@/lib/cx";

import styles from "./Logo.module.css";

/** 피그마 디자인 시스템 > 1. 앱 틀 > 로고. 그림(36×21) + "REMAKE". */
export function Logo({ className }: { className?: string }) {
  return (
    <span className={cx(styles.logo, className)}>
      <Image src="/images/logo-mark.png" alt="" width={36} height={21} priority />
      <span className={styles.text}>REMAKE</span>
    </span>
  );
}
