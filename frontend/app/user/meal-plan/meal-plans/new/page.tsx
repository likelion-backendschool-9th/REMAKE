import type { Metadata } from "next";

import { MealPlanSetupContainer } from "./_components/MealPlanSetupContainer/MealPlanSetupContainer";

export const metadata: Metadata = {
  title: "식단표 생성",
  robots: { index: false },
};

/** p5.1 식단표 생성 */
export default function MealPlanSetupPage() {
  return <MealPlanSetupContainer />;
}
