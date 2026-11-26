// 거의 모든 화면이 쓰는 묶음은 화면을 그리기 전에 받기 시작한다(next/font가 미리 받기 링크를 붙임)
import "./fonts/pretendard-preload";

import type { Metadata, Viewport } from "next";

import { AppProviders } from "@/providers/app-providers";

import { gmarketSans } from "./fonts/fonts";

// 본문 Pretendard: 글자 묶음(unicode-range)으로 나눈 파일이라 화면에 나온 글자가 든 묶음만 받는다(blueprint §6)
import "./fonts/pretendard.css";
// 받는 동안 보이는 기기 글꼴의 폭·높이를 Pretendard에 맞춘다
import "./fonts/pretendard-fallback.css";
// 문서 스크롤바(OverlayScrollbars, lib/page-scrollbar)
import "overlayscrollbars/overlayscrollbars.css";
import "./globals.css";
import styles from "./layout.module.css";

const APP_NAME = "REMAKE";

export const metadata: Metadata = {
  applicationName: APP_NAME,
  title: { default: APP_NAME, template: `%s | ${APP_NAME}` },
  description: "AI 기반 냉장고 식재료 관리 서비스",
  appleWebApp: { capable: true, statusBarStyle: "default", title: APP_NAME },
  formatDetection: { telephone: false },
};

export const viewport: Viewport = {
  themeColor: "#FFFFFF",
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    // data-overlayscrollbars-initialize: 스크립트가 스크롤바를 바꾸기 전 잠깐 기본 스크롤바가 보였다 사라지는 깜빡임을 막는다
    <html lang="ko" className={gmarketSans.variable} data-overlayscrollbars-initialize="">
      <body data-overlayscrollbars-initialize="">
        <AppProviders>
          <div className={styles.shell}>{children}</div>
        </AppProviders>
      </body>
    </html>
  );
}
