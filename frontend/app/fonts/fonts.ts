import localFont from "next/font/local";

// 본문 Pretendard는 여기서 불러오지 않는다. 글자 묶음별로 나눈 파일(pretendard 패키지의 dynamic subset)을
// app/layout.tsx에서 CSS로 불러온다. 굵기마다 한글 전체(약 770KB)를 미리 받지 않으려는 것이다.
// 거의 모든 화면이 쓰는 묶음만 pretendard-preload.ts(생성 파일)에서 next/font로 미리 받는다.

// 로고 글자 전용 폰트(globals.css의 --font-family-logo). 로고가 있는 화면에서만 쓰므로 미리 받지 않는다.
// 로고 글자는 "REMAKE"(헤더·로그인)와 "리메이크"(스플래시)뿐이라 원본(굵기마다 약 0.5MB, 한글 전체)에서
// 영문·숫자·기호(U+0020-007E)와 "리메이크" 네 글자만 남긴 파일을 쓴다(굵기마다 약 18KB). 로고에 새 글자를 쓰려면
// 원본을 fontTools(pyftsubset)로 다시 잘라야 한다. 원본이 SIL OFL 1.1이고 "Gmarket Sans Font"가 예약 글꼴 이름이라,
// 잘라낸 수정본은 글꼴 안 이름을 "REMAKE Logo"로 바꿨다(저작권·라이선스 정보는 그대로 들어 있음).
export const gmarketSans = localFont({
  src: [
    { path: "./GmarketSans-Light.logo.woff2", weight: "300", style: "normal" },
    { path: "./GmarketSans-Medium.logo.woff2", weight: "500", style: "normal" },
    { path: "./GmarketSans-Bold.logo.woff2", weight: "700", style: "normal" },
  ],
  variable: "--font-gmarket",
  display: "swap",
  preload: false,
  // 파일에 없는 글자(그 밖의 한글 등)는 이 폰트를 받지 않고 바로 다음 글꼴(Pretendard)로 그린다. 리·메·이·크
  declarations: [{ prop: "unicode-range", value: "U+0020-007E, U+B9AC, U+BA54, U+C774, U+D06C" }],
});
