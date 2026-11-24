import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { env } from "@/lib/env";

import { P1_STATES } from "./_lib/states";

import styles from "./page.module.css";

// 개발 환경 전용: p1 화면 상태 목록(blueprint §3.1 7번). 운영에서는 404.
export const metadata: Metadata = {
  title: "p1 상태 미리보기",
  robots: { index: false },
};

export default function P1PreviewIndexPage() {
  if (!env.isDev) notFound();

  return (
    <main className={styles.main}>
      <h1 className={styles.title}>p1 로그인·온보딩·설정 (개발용)</h1>
      <ul className={styles.list}>
        {P1_STATES.map((state) => (
          <li key={state.slug}>
            <Link href={`/dev/p1/${state.slug}`} className={styles.link}>
              <span className={styles.label}>{state.label}</span>
              <code className={styles.route}>{state.route}</code>
            </Link>
          </li>
        ))}
      </ul>
    </main>
  );
}
