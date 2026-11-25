import type { Metadata } from "next";

import { HouseholdContainer } from "./_components/HouseholdContainer/HouseholdContainer";

export const metadata: Metadata = {
  title: "가구원 수",
  robots: { index: false },
};

/** p1.1 가구원 수(온보딩) */
export default function OnboardingHouseholdPage() {
  return <HouseholdContainer />;
}
