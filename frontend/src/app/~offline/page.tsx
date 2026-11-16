import type { Metadata } from "next";

import styles from "../page.module.css";

export const metadata: Metadata = { title: "오프라인" };

export default function OfflinePage() {
  return (
    <main className={styles.main}>
      <h1 className={styles.title}>인터넷에 연결되어 있지 않아요</h1>
      <p className={styles.description}>연결 상태를 확인한 뒤 다시 시도해 주세요.</p>
    </main>
  );
}
