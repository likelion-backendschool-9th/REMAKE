import type { NextConfig } from "next";

import { withSerwist } from "@serwist/turbopack";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  // nginx 뒤에서 Node 서버로 띄우기 위한 독립 실행 빌드
  output: "standalone",
  turbopack: {
    resolveAlias: {
      // MSW 워커는 브라우저에서만 로드하고, 서버 번들에서는 빈 모듈로 대체
      "@/mocks/browser": { browser: "./src/mocks/browser.ts", default: "./src/mocks/noop.ts" },
    },
  },
};

export default withSerwist(nextConfig);
