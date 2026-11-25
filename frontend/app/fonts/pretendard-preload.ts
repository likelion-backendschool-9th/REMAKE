// 생성 파일: 직접 고치지 말 것. scripts/gen-pretendard-css.mjs (pnpm gen:fonts)
// 거의 모든 화면이 쓰는 Pretendard 1.3.9 묶음. app/layout.tsx에서 불러 모든 화면에 미리 받기가 붙는다.
// Pretendard: SIL Open Font License 1.1 (https://github.com/orioncactus/pretendard)

import localFont from "next/font/local";

export const pretendardSubset91 = localFont({
  src: [
    {
      path: "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/Pretendard-Regular.subset.91.woff2",
      weight: "400",
      style: "normal",
    },
    {
      path: "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/Pretendard-Medium.subset.91.woff2",
      weight: "500",
      style: "normal",
    },
    {
      path: "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/Pretendard-Bold.subset.91.woff2",
      weight: "700",
      style: "normal",
    },
  ],
  display: "swap",
  adjustFontFallback: false,
  declarations: [
    { prop: "font-family", value: "Pretendard" },
    {
      prop: "unicode-range",
      value:
        "U+20-22, U+27-2a, U+2c-39, U+3a-3b, U+3f, U+41-4e, U+4f-5d, U+61-7b, U+7d, U+a0-a1, U+ab, U+ad-ae, U+b7, U+bb, U+bf, U+2013-2014, U+201c-201d, U+2122, U+ac00, U+ace0, U+ae30, U+b2e4, U+b85c, U+b9ac, U+c0ac, U+c2a4, U+c2dc, U+c774, U+c778, U+c9c0, U+d558",
    },
  ],
});

export const pretendardSubset89 = localFont({
  src: [
    {
      path: "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/Pretendard-Medium.subset.89.woff2",
      weight: "500",
      style: "normal",
    },
    {
      path: "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/Pretendard-Bold.subset.89.woff2",
      weight: "700",
      style: "normal",
    },
  ],
  display: "swap",
  adjustFontFallback: false,
  declarations: [
    { prop: "font-family", value: "Pretendard" },
    {
      prop: "unicode-range",
      value:
        "U+d, U+7c, U+ac10, U+ac15, U+ac74, U+ac80, U+ac83, U+acc4, U+ad11, U+ad50, U+ad6d, U+adfc, U+ae00, U+ae08, U+ae4c, U+b0a8, U+b124, U+b144, U+b178, U+b274, U+b2a5, U+b2e8, U+b2f9, U+b354, U+b370, U+b418, U+b41c, U+b4f1, U+b514, U+b798, U+b808, U+b824-b825, U+b8cc, U+b978, U+b9d0, U+b9e4, U+baa9, U+bb3c, U+bc18, U+bc1c, U+bc30, U+bc84, U+bcf5, U+bcf8, U+bd84, U+be0c, U+be14, U+c0b0, U+c0c9, U+c0dd, U+c124, U+c2dd, U+c2e4, U+c2ec, U+c54c, U+c57c-c57d, U+c591, U+c5c5-c5c6, U+c5ed, U+c608, U+c640, U+c6b8, U+c6d4, U+c784, U+c7ac, U+c800-c801, U+c9c1, U+c9d1, U+cc28, U+cc98, U+cc9c, U+ccad, U+cd5c, U+cd94, U+cd9c, U+cde8, U+ce68, U+cf54, U+d0dc, U+d14c, U+d1a0, U+d1b5, U+d2f0, U+d30c, U+d310, U+d398, U+d45c, U+d50c, U+d53c, U+d560, U+d568, U+d589, U+d604, U+d6c4, U+d788",
    },
  ],
});

export const pretendardSubset90 = localFont({
  src: [
    {
      path: "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/Pretendard-Medium.subset.90.woff2",
      weight: "500",
      style: "normal",
    },
    {
      path: "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/Pretendard-Bold.subset.90.woff2",
      weight: "700",
      style: "normal",
    },
  ],
  display: "swap",
  adjustFontFallback: false,
  declarations: [
    { prop: "font-family", value: "Pretendard" },
    {
      prop: "unicode-range",
      value:
        "U+ac04, U+ac1c, U+ac70, U+ac8c, U+acbd, U+acf5, U+acfc, U+ad00, U+ad6c, U+adf8, U+b098, U+b0b4, U+b294, U+b2c8, U+b300, U+b3c4, U+b3d9, U+b4dc, U+b4e4, U+b77c, U+b7ec, U+b85d, U+b97c, U+b9c8, U+b9cc, U+ba54, U+ba74, U+ba85, U+baa8, U+bb34, U+bb38, U+bbf8, U+bc14, U+bc29, U+bc88, U+bcf4, U+bd80, U+be44, U+c0c1, U+c11c, U+c120, U+c131, U+c138, U+c18c, U+c218, U+c2b5, U+c2e0, U+c544, U+c548, U+c5b4, U+c5d0, U+c5ec, U+c5f0, U+c601, U+c624, U+c694, U+c6a9, U+c6b0, U+c6b4, U+c6d0, U+c704, U+c720, U+c73c, U+c740, U+c744, U+c74c, U+c758, U+c77c, U+c785, U+c788, U+c790-c791, U+c7a5, U+c804, U+c815, U+c81c, U+c870, U+c8fc, U+c911, U+c9c4, U+ccb4, U+ce58, U+ce74, U+d06c, U+d0c0, U+d130, U+d2b8, U+d3ec, U+d504, U+d55c, U+d569, U+d574, U+d638, U+d654, U+d68c",
    },
  ],
});
