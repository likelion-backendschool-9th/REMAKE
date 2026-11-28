import { Suspense } from "react";
import type { Metadata } from "next";

import { WeeklyRewardsContainer } from "./_components/WeeklyRewardsContainer/WeeklyRewardsContainer";

export const metadata: Metadata = {
  title: "랭킹·보상",
  description: "주간 공유 레시피 TOP 3와 보상 지급 이력을 확인합니다.",
  robots: { index: false },
};

export default function AdminWeeklyRewardsPage() {
  return (
    <Suspense>
      <WeeklyRewardsContainer />
    </Suspense>
  );
}
