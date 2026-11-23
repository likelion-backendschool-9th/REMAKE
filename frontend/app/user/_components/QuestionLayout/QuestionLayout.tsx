import type { ReactNode } from "react";

import { BottomBar } from "@/components/BottomBar/BottomBar";
import { SubHeader } from "@/components/SubHeader/SubHeader";

import styles from "./QuestionLayout.module.css";

type QuestionLayoutProps = {
  /** 화면 질문(h1). 예: "가구원 수를 알려주세요!" */
  title: string;
  children: ReactNode;
  /** 화면 아래 고정 버튼 */
  footer: ReactNode;
};

/**
 * 질문 하나를 묻는 화면의 틀: ← 헤더, 질문 제목, 본문, 아래 고정 버튼.
 * 온보딩(p1.1·p1.2)과 설정의 가구원 설정(p1.3.1)·알레르기 재료 제외(p1.3.2)가 쓴다.
 */
export function QuestionLayout({ title, children, footer }: QuestionLayoutProps) {
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
