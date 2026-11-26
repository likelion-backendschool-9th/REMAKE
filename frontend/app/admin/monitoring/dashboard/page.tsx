import { Suspense } from "react";
import type { Metadata } from "next";

import { DashboardContainer } from "./_components/DashboardContainer/DashboardContainer";

export const metadata: Metadata = {
  title: "대시보드",
  robots: { index: false },
};

/** a1 대시보드 */
export default function AdminDashboardPage() {
  // 검색·기간 조건을 주소(nuqs)에서 읽으므로 Suspense 안에 둔다(cacheComponents, blueprint §5)
  return (
    <Suspense>
      <DashboardContainer />
    </Suspense>
  );
}
