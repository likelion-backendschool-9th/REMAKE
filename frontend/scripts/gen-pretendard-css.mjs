// pretendard 패키지의 dynamic subset CSS에서 우리가 쓰는 굵기·woff2만 남겨 두 파일을 만든다.
// - app/fonts/pretendard.css: 화면에 나온 글자가 든 묶음만 그때 받는 @font-face
// - app/fonts/pretendard-preload.ts: 거의 모든 화면이 쓰는 묶음(PRELOAD). next/font/local로 불러
//   <link rel="preload">가 붙어 화면을 그리기 전에 받기 시작한다. 이 묶음은 pretendard.css에서 뺀다.
// 굵기·미리 받을 묶음을 바꾸거나 pretendard 버전을 올리면 다시 실행한다: pnpm gen:fonts
// 쓰는 굵기: 400(content-regular·error), 500(title·content·assist), 700(display·header 등), 800(마트 배지)

import { readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import * as prettier from "prettier";

const WEIGHTS = new Set(["400", "500", "700", "800"]);

// 미리 받을 묶음: 묶음 번호 → 굵기. 개발 미리보기 17개 화면 중 16개 이상에서 쓰인 묶음(2026-11-26 측정).
// 91은 영문·숫자·기호와 가장 자주 쓰는 한글, 89·90은 그다음으로 자주 쓰는 한글이다. 7개 파일 약 98KB.
const PRELOAD = new Map([
  ["91", ["400", "500", "700"]],
  ["89", ["500", "700"]],
  ["90", ["500", "700"]],
]);

const require = createRequire(import.meta.url);
const source = require.resolve("pretendard/dist/web/static/pretendard-dynamic-subset.css");
const css = readFileSync(source, "utf8");
const version = JSON.parse(
  readFileSync(join(dirname(require.resolve("pretendard/package.json")), "package.json"), "utf8"),
).version;

const FILE_DIR = "../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/";

const faces = (css.match(/@font-face\s*{[^}]*}/g) ?? [])
  .map((face) => ({
    text: face,
    weight: /font-weight:\s*(\d+)/.exec(face)?.[1] ?? "",
    subset: /subset\.(\d+)\.woff2/.exec(face)?.[1] ?? "",
    file: /url\(\.\/woff2-dynamic-subset\/([^)]+\.woff2)\)/.exec(face)?.[1] ?? "",
    range: /unicode-range:\s*([^;]+);/.exec(face)?.[1].trim() ?? "",
  }))
  .filter((face) => WEIGHTS.has(face.weight));

const isPreloaded = (face) => PRELOAD.get(face.subset)?.includes(face.weight) ?? false;

const kept = faces
  .filter((face) => !isPreloaded(face))
  .map((face) =>
    face.text
      // woff 대체 파일은 빼고 woff2만 남긴다(지원 브라우저는 모두 woff2를 읽음)
      .replace(/,\s*url\([^)]*\.woff\)\s*format\('woff'\)/, "")
      .replace(/url\(\.\/woff2-dynamic-subset\//, `url(${FILE_DIR}`),
  );

const header = `/* 생성 파일: 직접 고치지 말 것. scripts/gen-pretendard-css.mjs (pnpm gen:fonts)
   pretendard ${version} dynamic subset, 굵기 ${[...WEIGHTS].join("/")}, woff2만.
   글자 묶음(unicode-range)마다 파일이 나뉘어 있어 화면에 나온 글자가 든 묶음만 받는다.
   미리 받는 묶음(${[...PRELOAD].map(([subset, weights]) => `${subset}: ${weights.join("/")}`).join(", ")})은 pretendard-preload.ts에 있다.
   Pretendard: SIL Open Font License 1.1 (https://github.com/orioncactus/pretendard) */
`;

writeFileSync(
  new URL("../app/fonts/pretendard.css", import.meta.url),
  `${header}\n${kept.join("\n")}\n`,
);

// next/font/local은 값을 코드에 그대로 적어야 해서(정적 분석) 묶음마다 호출을 하나씩 만든다.
// font-family를 Pretendard로 바꿔 pretendard.css의 나머지 묶음과 한 글꼴로 이어 쓴다.
// 대체 글꼴 크기 맞춤은 globals.css의 Pretendard Fallback이 맡으므로 next/font 것은 끈다.
const preloadFaces = [...PRELOAD.keys()].map((subset) => {
  const group = faces.filter((face) => face.subset === subset && isPreloaded(face));
  const ranges = new Set(group.map((face) => face.range));
  if (group.length !== PRELOAD.get(subset).length || ranges.size !== 1) {
    throw new Error(`묶음 ${subset}: 굵기별 파일이나 글자 범위가 예상과 다르다`);
  }
  const src = group
    .map((face) => `{ path: "${FILE_DIR}${face.file}", weight: "${face.weight}", style: "normal" }`)
    .join(",\n");
  return `export const pretendardSubset${subset} = localFont({
  src: [${src}],
  display: "swap",
  adjustFontFallback: false,
  declarations: [
    { prop: "font-family", value: "Pretendard" },
    { prop: "unicode-range", value: "${[...ranges][0]}" },
  ],
});`;
});

const preloadPath = fileURLToPath(new URL("../app/fonts/pretendard-preload.ts", import.meta.url));
const preloadCode = `// 생성 파일: 직접 고치지 말 것. scripts/gen-pretendard-css.mjs (pnpm gen:fonts)
// 거의 모든 화면이 쓰는 Pretendard ${version} 묶음. app/layout.tsx에서 불러 모든 화면에 미리 받기가 붙는다.
// Pretendard: SIL Open Font License 1.1 (https://github.com/orioncactus/pretendard)

import localFont from "next/font/local";

${preloadFaces.join("\n\n")}
`;
writeFileSync(
  preloadPath,
  await prettier.format(preloadCode, {
    ...(await prettier.resolveConfig(preloadPath)),
    filepath: preloadPath,
  }),
);

process.stdout.write(
  `app/fonts/pretendard.css: @font-face ${kept.length}개 (원본 ${faces.length}개 중 미리 받기 ${faces.length - kept.length}개 제외)\n` +
    `app/fonts/pretendard-preload.ts: 묶음 ${PRELOAD.size}개\n`,
);
