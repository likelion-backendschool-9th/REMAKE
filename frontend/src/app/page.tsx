import styles from "./page.module.css";

export default function Home() {
  return (
    <main className={styles.main}>
      <h1 className={styles.title}>REMAKE</h1>
      <p className={styles.description}>냉장고 식재료 관리 서비스</p>
    </main>
  );
}
