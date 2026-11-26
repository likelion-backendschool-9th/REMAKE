import type { Metadata } from "next";

import { DashboardContainer } from "./_components/DashboardContainer/DashboardContainer";

export const metadata: Metadata = {
  title: "대시보드",
  robots: { index: false },
};

/** a1 대시보드 */
export default function AdminDashboardPage() {
  return <DashboardContainer />;
}
