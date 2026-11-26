import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { env } from "@/lib/env";

import { ADMIN_STATES } from "./_lib/states";

import styles from "./page.module.css";

// 개발 환경 전용: 운영자 페이지 화면 상태 목록(blueprint §3.1 7번). 운영에서는 404.
export const metadata: Metadata = {
  title: "운영자 상태 미리보기",
  robots: { index: false },
};

export default function AdminPreviewIndexPage() {
  if (!env.isDev) notFound();

  return (
    <main className={styles.main}>
      <h1 className={styles.title}>운영자 페이지 (개발용)</h1>
      <ul className={styles.list}>
        {ADMIN_STATES.map((state) => (
          <li key={state.slug}>
            <Link href={`/dev/admin/${state.slug}`} className={styles.link}>
              <span className={styles.label}>{state.label}</span>
              <code className={styles.route}>{state.route}</code>
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
