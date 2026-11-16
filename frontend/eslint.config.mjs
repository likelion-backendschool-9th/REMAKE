import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import prettier from "eslint-config-prettier/flat";
import simpleImportSort from "eslint-plugin-simple-import-sort";

const eslintConfig = defineConfig([
  // Next.js 권장 규칙 (React, Hooks, a11y, Core Web Vitals, TypeScript)
  ...nextVitals,
  ...nextTs,

  {
    plugins: { "simple-import-sort": simpleImportSort },
    rules: {
      // ===== import =====
      // 순서: 사이드이펙트 → node: 내장 → react/next → 외부 패키지 → @/ 내부 → 상대 경로 → 스타일
      "simple-import-sort/imports": [
        "error",
        {
          groups: [
            ["^\\u0000(?!.*\\.css$)"],
            ["^node:"],
            // type import는 끝에 \u0000 이 붙어 비교되므로 함께 허용
            ["^react(-dom)?(/.*)?\\u0000?$", "^next(/.*)?\\u0000?$"],
            ["^@?\\w"],
            ["^@/"],
            ["^\\.\\.(?!/?$)", "^\\.\\./?$", "^\\./(?=.*/)(?!/?$)", "^\\.(?!/?$)", "^\\./?$"],
            ["^.+\\.css$", "^\\u0000.+\\.css$"],
          ],
        },
      ],
      "simple-import-sort/exports": "error",
      "import/first": "error",
      "import/newline-after-import": "error",
      "import/no-duplicates": "error",

      // ===== TypeScript =====
      "@typescript-eslint/consistent-type-imports": [
        "error",
        { prefer: "type-imports", fixStyle: "inline-type-imports" },
      ],
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_", caughtErrorsIgnorePattern: "^_" },
      ],
      "@typescript-eslint/no-explicit-any": "error",

      // ===== 일반 =====
      "no-console": ["warn", { allow: ["warn", "error"] }],
      eqeqeq: ["error", "always", { null: "ignore" }],
      "prefer-const": "error",
      "object-shorthand": "error",

      // ===== React =====
      "react/self-closing-comp": "error",
      "react/jsx-boolean-value": ["error", "never"],
      "react/jsx-curly-brace-presence": ["error", { props: "never", children: "never" }],

      // ===== 프로젝트 규칙 (blueprint.md) =====
      "no-restricted-imports": [
        "error",
        {
          paths: [
            {
              name: "dayjs",
              message: "@/lib/dayjs 에서 import 하세요 (ko 로케일·플러그인 설정됨).",
            },
            { name: "lucide-react", message: "아이콘은 @solar-icons/react 를 사용합니다." },
            { name: "clsx", message: "스타일은 CSS Modules 로 작성합니다." },
          ],
          patterns: [
            {
              group: ["tailwindcss", "@tailwindcss/*", "styled-components", "@emotion/*"],
              message: "스타일은 CSS Modules + CSS 변수로만 작성합니다.",
            },
            {
              // 코로케이션: 라우트 폴더 코드는 그 라우트(와 하위 라우트)에서 상대 경로로만 쓴다
              group: ["@/app", "@/app/**"],
              message:
                "다른 라우트의 코드는 import 하지 않습니다. 여러 라우트가 쓰면 components/, hooks/, lib/ 로 올리세요.",
            },
            {
              group: ["@solar-icons/react"],
              message: "스타일별 경로에서 import 하세요. 예: @solar-icons/react/linear",
            },
          ],
        },
      ],
      "no-restricted-properties": [
        "error",
        {
          object: "process",
          property: "env",
          message: "환경 변수는 @/lib/env 에서만 읽습니다.",
        },
      ],
    },
  },

  // 예외: 설정 파일 위치에서는 원래 패키지를 직접 써야 함
  {
    files: ["lib/dayjs.ts"],
    rules: { "no-restricted-imports": "off" },
  },
  {
    files: ["lib/env.ts", "providers/**", "*.config.*"],
    rules: { "no-restricted-properties": "off" },
  },
  // 포맷 관련 규칙은 끄고 Prettier에 맡김 (항상 마지막)
  prettier,

  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "public/mockServiceWorker.js",
    "lib/api/schema.d.ts",
  ]),
]);

export default eslintConfig;
