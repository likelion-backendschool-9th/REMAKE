// pretendard 패키지의 dynamic subset CSS에서 우리가 쓰는 굵기·woff2만 남겨 app/fonts/pretendard.css를 만든다.
// 굵기를 바꾸거나 pretendard 버전을 올리면 다시 실행한다: pnpm gen:fonts
// 쓰는 굵기: 400(content-regular·error), 500(title·content·assist), 700(display·header 등), 800(마트 배지)

import { readFileSync, writeFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";

const WEIGHTS = new Set(["400", "500", "700", "800"]);

const require = createRequire(import.meta.url);
const source = require.resolve("pretendard/dist/web/static/pretendard-dynamic-subset.css");
const css = readFileSync(source, "utf8");
const version = JSON.parse(
  readFileSync(join(dirname(require.resolve("pretendard/package.json")), "package.json"), "utf8"),
).version;

const faces = css.match(/@font-face\s*{[^}]*}/g) ?? [];
const kept = faces
  .filter((face) => WEIGHTS.has(/font-weight:\s*(\d+)/.exec(face)?.[1] ?? ""))
  .map((face) =>
    face
      // woff 대체 파일은 빼고 woff2만 남긴다(지원 브라우저는 모두 woff2를 읽음)
      .replace(/,\s*url\([^)]*\.woff\)\s*format\('woff'\)/, "")
      .replace(
        /url\(\.\/woff2-dynamic-subset\//,
        "url(../../node_modules/pretendard/dist/web/static/woff2-dynamic-subset/",
      ),
  );

const header = `/* 생성 파일: 직접 고치지 말 것. scripts/gen-pretendard-css.mjs (pnpm gen:fonts)
   pretendard ${version} dynamic subset, 굵기 ${[...WEIGHTS].join("/")}, woff2만.
   글자 묶음(unicode-range)마다 파일이 나뉘어 있어 화면에 나온 글자가 든 묶음만 받는다.
   Pretendard: SIL Open Font License 1.1 (https://github.com/orioncactus/pretendard) */
`;

writeFileSync(
  new URL("../app/fonts/pretendard.css", import.meta.url),
  `${header}\n${kept.join("\n")}\n`,
);
process.stdout.write(
  `app/fonts/pretendard.css: @font-face ${kept.length}개 (원본 ${faces.length}개)\n`,
);
