import type { Metadata } from "next";

import { AllergiesContainer } from "./_components/AllergiesContainer/AllergiesContainer";

export const metadata: Metadata = {
  title: "제외할 재료 선택",
  robots: { index: false },
};

/** p1.2 알레르기 선택(온보딩) */
export default function OnboardingAllergiesPage() {
  return <AllergiesContainer />;
}
