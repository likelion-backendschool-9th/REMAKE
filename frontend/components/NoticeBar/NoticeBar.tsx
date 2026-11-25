import Link from "next/link";

import { CountBadge } from "@/components/CountBadge/CountBadge";
import { cx } from "@/lib/cx";

import styles from "./NoticeBar.module.css";

type NoticeBarProps = {
  /** 가장 급한 알림 한 줄(서버가 주는 문구) */
  message: string;
  /** 안 읽은 알림 개수 */
  unreadCount: number;
  /** 누르면 갈 알림 화면 주소 */
  href: string;
  className?: string;
};

/** 피그마 디자인 시스템 > 1. 앱 틀 > 알림 띠. 메인 헤더 안 가장 급한 알림 한 줄 + 개수. */
export function NoticeBar({ message, unreadCount, href, className }: NoticeBarProps) {
  return (
    <Link href={href} className={cx(styles.bar, className)}>
      <span className={styles.message}>{message}</span>
      {unreadCount > 0 && (
        <CountBadge count={unreadCount} label={`안 읽은 알림 ${unreadCount}개`} />
      )}
    </Link>
  );
}
