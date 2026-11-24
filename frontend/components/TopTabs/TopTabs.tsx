import Link from "next/link";

import { cx } from "@/lib/cx";

import styles from "./TopTabs.module.css";

export type TopTab = { href: string; label: string };

type TopTabsProps = {
  tabs: readonly TopTab[];
  /** 지금 화면의 탭 href */
  activeHref: string;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 1. 앱 틀 > 상단 탭(+ 상단 탭/항목). 메인 헤더 아래 화면 이동 탭.
 * 화면을 옮기는 링크이므로 tablist가 아니라 nav + aria-current로 그린다. 선택=굵은 글자(밑줄 없음).
 */
export function TopTabs({ tabs, activeHref, className }: TopTabsProps) {
  return (
    <nav className={cx(styles.tabs, className)} aria-label="주요 메뉴">
      {tabs.map((tab) => (
        <Link
          key={tab.href}
          href={tab.href}
          className={styles.tab}
          aria-current={tab.href === activeHref ? "page" : undefined}
        >
          {tab.label}
        </Link>
      ))}
    </nav>
  );
}
