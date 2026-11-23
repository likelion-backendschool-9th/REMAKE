import type { Metadata, Viewport } from "next";

import { AppProviders } from "@/providers/app-providers";

import { gmarketSans } from "./fonts/fonts";

// 본문 Pretendard: 글자 묶음(unicode-range)으로 나눈 파일이라 화면에 나온 글자가 든 묶음만 받는다(blueprint §6)
import "./fonts/pretendard.css";
import "./globals.css";

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
    <html lang="ko" className={gmarketSans.variable}>
      <body>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  );
}
