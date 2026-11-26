"use client";

import { useRouter } from "next/navigation";

import { Button } from "@/components/Button/Button";
import { Logo } from "@/components/Logo/Logo";
import { ResultState } from "@/components/ResultState/ResultState";

import { SERVICE_HOME_PATH } from "../../_lib/admin-menu";

import styles from "./AdminForbidden.module.css";

/**
 * a0-권한 없음. ADMIN이 아닌 계정으로 운영자 화면에 들어왔을 때(서버 403) 사이드 메뉴 없이 화면 가운데에 안내한다.
 * "서비스로 돌아가기"는 로그인 뒤 첫 화면(냉장고)으로 간다
 */
export function AdminForbidden() {
  const router = useRouter();

  return (
    <main className={styles.page}>
      <Logo className={styles.logo} />
      <div className={styles.content}>
        <ResultState
          variant="error"
          title="운영자만 들어올 수 있어요"
          description="ADMIN 권한이 있는 계정으로 로그인해 주세요."
          action={<Button onClick={() => router.push(SERVICE_HOME_PATH)}>서비스로 돌아가기</Button>}
        />
      </div>
    </main>
  );
}
