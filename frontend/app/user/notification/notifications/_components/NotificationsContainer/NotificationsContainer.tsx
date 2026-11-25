"use client";

import { useState } from "react";
import Link from "next/link";

import { EmptyState } from "@/components/EmptyState/EmptyState";
import { Skeleton } from "@/components/Skeleton/Skeleton";
import { SubHeader } from "@/components/SubHeader/SubHeader";

import { getNotificationHref, groupByDay, type NotificationItem } from "../../_lib/notifications";

import styles from "./NotificationsContainer.module.css";

type NotificationsContainerProps = {
  /** 알림 목록(API 44, 최신순). 없으면 불러오는 중. 마크업 확인용 값은 미리보기(app/dev)에서만 넘긴다 */
  notifications?: NotificationItem[];
};

/**
 * p6 알림. "오늘", "1일 전" … 묶음으로 보인다(domain-rules 6).
 * 소비기한 알림은 누르는 동작이 없고, 식단표 완성·실패 알림은 식단표 화면으로 간다.
 * 화면을 열면 서버가 모두 읽음 처리한다(개별·모두 읽음 버튼 없음).
 * 상태: 목록(p6-알람) / 불러오는 중(p6-알람-스켈레톤) / 빈 목록(피그마 없음).
 */
export function NotificationsContainer({ notifications }: NotificationsContainerProps) {
  return (
    <>
      <SubHeader title="알림" />
      <main className={styles.main} aria-busy={!notifications || undefined}>
        {!notifications ? (
          <section className={styles.group}>
            {/* 피그마 p6-알람-스켈레톤: "오늘" 제목 아래 줄 자리 2개 */}
            <h2 className={styles.groupTitle}>오늘</h2>
            <div className={styles.skeletons}>
              <Skeleton width="100%" height="2.25rem" />
              <Skeleton width="100%" height="2.25rem" />
            </div>
          </section>
        ) : notifications.length === 0 ? (
          // 빈 목록 화면은 피그마에 아직 없다(domain-rules 6 "screen pending"). 문구는 임시
          <EmptyState>받은 알림이 없어요</EmptyState>
        ) : (
          <NotificationGroups notifications={notifications} />
        )}
      </main>
    </>
  );
}

/**
 * 알림 묶음 목록. 묶음 기준 "오늘"은 이 목록을 처음 그린 순간으로 고정한다.
 * 알림이 있을 때만 그려지므로, 미리 그려 두는 빈 화면(불러오는 중)에서는 시각을 읽지 않는다(cacheComponents)
 */
function NotificationGroups({ notifications }: { notifications: NotificationItem[] }) {
  const [now] = useState(() => new Date());
  return groupByDay(notifications, now).map((group) => (
    <section key={group.label} className={styles.group}>
      <h2 className={styles.groupTitle}>{group.label}</h2>
      <ul className={styles.list}>
        {group.items.map((item) => {
          const href = getNotificationHref(item);
          return (
            <li key={item.id} className={styles.item}>
              {href ? (
                <Link href={href} className={styles.link}>
                  {item.message}
                </Link>
              ) : (
                item.message
              )}
            </li>
          );
        })}
      </ul>
    </section>
  ));
}
