import localFont from "next/font/local";

// 본문 Pretendard는 여기서 불러오지 않는다. 글자 묶음별로 나눈 파일(pretendard 패키지의 dynamic subset)을
// app/layout.tsx에서 CSS로 불러온다. 굵기마다 한글 전체(약 770KB)를 미리 받지 않으려는 것이다.

// 로고 글자 전용 폰트(globals.css의 --font-family-logo). 로고가 있는 화면에서만 쓰므로 미리 받지 않는다.
export const gmarketSans = localFont({
  src: [
    { path: "./GmarketSans-Light.woff2", weight: "300", style: "normal" },
    { path: "./GmarketSans-Medium.woff2", weight: "500", style: "normal" },
    { path: "./GmarketSans-Bold.woff2", weight: "700", style: "normal" },
  ],
  variable: "--font-gmarket",
  display: "swap",
  preload: false,
});
