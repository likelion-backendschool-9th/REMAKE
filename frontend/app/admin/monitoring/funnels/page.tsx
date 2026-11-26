import { Suspense } from "react";
import type { Metadata } from "next";

import { FunnelContainer } from "./_components/FunnelContainer/FunnelContainer";

export const metadata: Metadata = {
  title: "퍼널",
  robots: { index: false },
};

/** a2 퍼널 */
export default function AdminFunnelsPage() {
  // 검색·기간 조건을 주소(nuqs)에서 읽으므로 Suspense 안에 둔다(cacheComponents, blueprint §5)
  return (
    <Suspense>
      <FunnelContainer />
    </Suspense>
  );
}
