import type { Metadata } from "next";
import Image from "next/image";

import { LoginActions } from "./_components/LoginActions/LoginActions";

import styles from "./page.module.css";

export const metadata: Metadata = {
  title: "로그인",
  description: "카카오·네이버·구글 계정으로 REMAKE를 시작하세요.",
};

/** p1 로그인. 화면 가운데에 로고와 소셜 로그인 버튼, 약관 링크. */
export default function LoginPage() {
  return (
    <main className={styles.main}>
      <div className={styles.content}>
        <h1 className={styles.logo}>
          <Image src="/images/logo-mark.png" alt="" width={110} height={65} priority />
          REMAKE
        </h1>
        <LoginActions />
      </div>
    </main>
  );
}
