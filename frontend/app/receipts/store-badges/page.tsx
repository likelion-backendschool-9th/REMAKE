import type { Metadata } from "next";
import { notFound } from "next/navigation";

import { env } from "@/lib/env";

import { StoreBadge } from "../_components/StoreBadge/StoreBadge";
import { STORE_CODES, STORES } from "../_components/StoreBadge/stores";

import styles from "./page.module.css";

// 개발 환경 전용 확인 화면: 마트 배지 15개를 피그마와 나란히 비교한다. 운영에서는 404.
export const metadata: Metadata = {
  title: "마트 배지 확인",
  robots: { index: false },
};

export default function StoreBadgesPreviewPage() {
  if (!env.isDev) notFound();

  return (
    <main className={styles.main}>
      <h1 className={styles.title}>마트 배지 (개발용)</h1>
      <ul className={styles.list}>
        {STORE_CODES.map((code) => (
          <li key={code} className={styles.row}>
            <StoreBadge storeCode={code} size={32} />
            <StoreBadge storeCode={code} />
            <StoreBadge storeCode={code} size={56} />
            <div className={styles.info}>
              <span className={styles.name}>{STORES[code].name}</span>
              <code className={styles.meta}>
                {code} · {STORES[code].bg} / {STORES[code].fg}
              </code>
            </div>
          </li>
        ))}
        <li className={styles.row}>
          <StoreBadge storeCode={null} />
          <div className={styles.info}>
            <span className={styles.name}>null → ETC</span>
          </div>
        </li>
        <li className={styles.row}>
          <StoreBadge storeCode="UNKNOWN_MART" />
          <div className={styles.info}>
            <span className={styles.name}>모르는 코드 → ETC</span>
          </div>
        </li>
      </ul>
    </main>
  );
}
