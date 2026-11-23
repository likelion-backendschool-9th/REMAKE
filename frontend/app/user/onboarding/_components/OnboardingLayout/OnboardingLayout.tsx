import type { ReactNode } from "react";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { SubHeader } from "@/components/SubHeader/SubHeader";

import styles from "./OnboardingLayout.module.css";

type OnboardingLayoutProps = {
  /** 화면 질문(h1). 예: "가구원 수를 알려주세요!" */
  title: string;
  children: ReactNode;
  /** 화면 아래 고정 버튼 */
  footer: ReactNode;
};

/** 온보딩 단계 화면의 틀: ← 헤더, 질문 제목, 본문, 아래 고정 버튼. 건너뛰기 버튼은 없다(정책). */
export function OnboardingLayout({ title, children, footer }: OnboardingLayoutProps) {
  return (
    <>
      <SubHeader />
      <main className={styles.main}>
        <h1 className={styles.title}>{title}</h1>
        {children}
      </main>
      <BottomBar>{footer}</BottomBar>
    </>
  );
}
