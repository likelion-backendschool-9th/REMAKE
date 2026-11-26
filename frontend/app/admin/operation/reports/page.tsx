import { Suspense } from "react";
import type { Metadata } from "next";

import { ReportListContainer } from "./_components/ReportListContainer/ReportListContainer";

export const metadata: Metadata = {
  title: "신고 관리",
  robots: { index: false },
};

/** a4 신고 관리 */
export default function AdminReportsPage() {
  // 처리 대기·처리 완료 탭을 주소(nuqs)에서 읽으므로 Suspense 안에 둔다(cacheComponents, blueprint §5)
  return (
    <Suspense>
      <ReportListContainer />
    </Suspense>
  );
}
