import type { ComponentProps } from "react";

import { SettingsIcon } from "@solar-icons/react/outline";

import { IconButton } from "@/components/IconButton/IconButton";
import { Logo } from "@/components/Logo/Logo";
import { NoticeBar } from "@/components/NoticeBar/NoticeBar";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { type TopTab, TopTabs } from "@/components/TopTabs/TopTabs";
import { cx } from "@/lib/cx";

import styles from "./MainHeader.module.css";

type MainHeaderProps = {
  tabs: readonly TopTab[];
  activeHref: string;
  settingsHref: string;
  /** 피그마 알림=있음. 없으면 알림 띠를 그리지 않는다. */
  notice?: ComponentProps<typeof NoticeBar>;
  /** 알림을 불러오는 중이면 알림 띠 자리에 스켈레톤(피그마 p2-냉장고-스켈레톤) */
  isNoticeLoading?: boolean;
  className?: string;
};

/**
 * 피그마 디자인 시스템 > 1. 앱 틀 > 메인 헤더. 냉장고·영수증·레시피·식단표·장바구니 화면 맨 위.
 * 로고 줄(로고 + 설정) / 알림 띠(있을 때) / 상단 탭.
 */
export function MainHeader({
  tabs,
  activeHref,
  settingsHref,
  notice,
  isNoticeLoading = false,
  className,
}: MainHeaderProps) {
  return (
    <header className={cx(styles.header, className)}>
      <div className={styles.logoRow}>
        <Logo />
        <IconButton icon={<SettingsIcon />} label="설정" href={settingsHref} />
      </div>
      {isNoticeLoading ? (
        <div className={styles.noticeRow}>
          <Skeleton width="100%" height="2.375rem" radius="var(--radius-lg)" />
        </div>
      ) : (
        notice && (
          <div className={styles.noticeRow}>
            <NoticeBar {...notice} />
          </div>
        )
      )}
      <TopTabs tabs={tabs} activeHref={activeHref} />
    </header>
  );
}
