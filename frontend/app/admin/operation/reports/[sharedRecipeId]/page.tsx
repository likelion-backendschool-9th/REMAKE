import type { Metadata } from "next";

import { ReportDetailContainer } from "./_components/ReportDetailContainer/ReportDetailContainer";

export const metadata: Metadata = {
  title: "신고 상세",
  robots: { index: false },
};

/** a4.1 신고 상세. TODO: API 66을 붙일 때 Suspense 안에서 params.sharedRecipeId를 읽어 넘긴다 */
export default function AdminReportDetailPage() {
  return <ReportDetailContainer />;
}
