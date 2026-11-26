"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import { Badge } from "@/components/Badge/Badge";
import { Logo } from "@/components/Logo/Logo";
import { TextButton } from "@/components/TextButton/TextButton";

import {
  ADMIN_MENU_GROUPS,
  type AdminMenuKey,
  findActiveMenu,
  SERVICE_HOME_PATH,
} from "../../_lib/admin-menu";

import styles from "./AdminLayout.module.css";

const MAIN_ID = "admin-main";

type AdminLayoutProps = {
  /** 지금 보고 있는 메뉴(파랑 배경). 없으면 지금 주소로 정한다. 미리보기(app/dev)처럼 주소가 다를 때만 넘긴다 */
  activeMenu?: AdminMenuKey;
  /** 처리 대기 신고 수. 1 이상이면 "신고 관리" 옆에 빨간 배지로 보인다 */
  pendingReportCount?: number;
  children: ReactNode;
};

/**
 * 운영자 공통 레이아웃: 왼쪽 사이드 메뉴(232) + 오른쪽 본문. 피그마 운영자 페이지의 모든 화면이 같은 틀을 쓴다.
 * 본문 맨 위 페이지 제목은 AdminPageHeader로 각 화면이 그린다. app/admin/layout.tsx가 모든 /admin 화면에 씌운다.
 */
export function AdminLayout({ activeMenu, pendingReportCount = 0, children }: AdminLayoutProps) {
  const pathname = usePathname();
  const currentMenu = activeMenu ?? findActiveMenu(pathname);

  return (
    <div className={styles.layout}>
      {/* 키보드 사용자가 반복되는 사이드 메뉴를 건너뛰고 본문으로 가는 링크(blueprint §6.2). 포커스를 받을 때만 보인다 */}
      <a href={`#${MAIN_ID}`} className={styles.skipLink}>
        본문 바로가기
      </a>
      <nav className={styles.side} aria-label="운영자 메뉴">
        <div className={styles.brand}>
          <Logo />
          <Badge tone="primary">운영자</Badge>
        </div>

        {ADMIN_MENU_GROUPS.map((group) => (
          <div key={group.label} className={styles.group}>
            <p className={styles.groupTitle}>{group.label}</p>
            <ul className={styles.menu}>
              {group.items.map((item) => {
                const isActive = item.key === currentMenu;
                const content = (
                  <>
                    {item.label}
                    {item.key === "reports" && pendingReportCount > 0 && (
                      <Badge tone="danger">
                        <span className={styles.srOnly}>처리 대기</span>
                        {pendingReportCount}
                      </Badge>
                    )}
                  </>
                );
                return (
                  <li key={item.key}>
                    <Link
                      href={item.href}
                      className={styles.item}
                      aria-current={isActive ? "page" : undefined}
                    >
                      {content}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}

        <div className={styles.account}>
          <p className={styles.accountName}>운영자 계정</p>
          <p className={styles.accountDescription}>소셜 로그인 · ADMIN 권한</p>
          <TextButton tone="neutral" withArrow href={SERVICE_HOME_PATH} className={styles.back}>
            서비스로 돌아가기
          </TextButton>
        </div>
      </nav>

      <main id={MAIN_ID} className={styles.main} tabIndex={-1}>
        {children}
      </main>
    </div>
  );
}
