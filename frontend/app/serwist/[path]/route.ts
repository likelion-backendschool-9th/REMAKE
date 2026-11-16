import { spawnSync } from "node:child_process";

import { createSerwistRoute } from "@serwist/turbopack";

// 배포마다 오프라인 페이지 캐시를 갱신하기 위한 revision
const revision =
  spawnSync("git", ["rev-parse", "HEAD"], { encoding: "utf-8" }).stdout?.trim() ||
  crypto.randomUUID();

const serwist = createSerwistRoute({
  additionalPrecacheEntries: [{ url: "/~offline", revision }],
  swSrc: "app/sw.ts",
  useNativeEsbuild: true,
});

// 빌드할 때 sw.js 등을 미리 생성한다.
export const { generateStaticParams } = serwist;

// cacheComponents 에서는 Serwist 가 내보내는 dynamic = "force-static" 이 무시된다.
// 그대로 쓰면 esbuild 번들링이 끝나는 시점에 따라 빌드마다 정적(●)/동적(ƒ)이 바뀌고,
// 동적이 되면 standalone 운영 서버에 esbuild 가 없어 /serwist/sw.js 가 500 을 낸다.
// 번들링을 "use cache" 함수 안에서 해서 항상 빌드 때 미리 생성되게 한다. (blueprint.md §7)
async function getSerwistFile(path: string) {
  "use cache";
  const res = await serwist.GET(new Request("http://localhost"), {
    params: Promise.resolve({ path }),
  });
  return { body: await res.text(), contentType: res.headers.get("Content-Type") ?? "text/plain" };
}

// Serwist 가 만드는 파일. 그 밖의 경로는 런타임에 번들링을 시도하지 않고 404 로 끝낸다.
const SERWIST_FILES = new Set(["sw.js", "sw.js.map"]);

export async function GET(_request: Request, { params }: RouteContext<"/serwist/[path]">) {
  const { path } = await params;
  if (!SERWIST_FILES.has(path)) {
    return new Response("Not Found", { status: 404 });
  }
  const { body, contentType } = await getSerwistFile(path);
  return new Response(body, {
    headers: { "Content-Type": contentType, "Service-Worker-Allowed": "/" },
  });
}
