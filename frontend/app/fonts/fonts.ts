import localFont from "next/font/local";

// 본문 폰트. tokens.css의 --font-family-base가 --font-pretendard를 먼저 찾는다.
// 피그마에서 쓰는 굵기만 둔다: Regular 400(Remake/content-regular·error 텍스트 스타일), Medium 500, SemiBold 600, Bold 700, ExtraBold 800.
export const pretendard = localFont({
  src: [
    { path: "./Pretendard-Regular.woff2", weight: "400", style: "normal" },
    { path: "./Pretendard-Medium.woff2", weight: "500", style: "normal" },
    { path: "./Pretendard-SemiBold.woff2", weight: "600", style: "normal" },
    { path: "./Pretendard-Bold.woff2", weight: "700", style: "normal" },
    { path: "./Pretendard-ExtraBold.woff2", weight: "800", style: "normal" },
  ],
  variable: "--font-pretendard",
  display: "swap",
});

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
