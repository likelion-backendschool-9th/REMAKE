import type { Metadata } from "next";

import { MealPlanContainer } from "./_components/MealPlanContainer/MealPlanContainer";

export const metadata: Metadata = {
  title: "식단표",
  robots: { index: false },
};

/** p5 식단표 */
export default function MealPlanPage() {
  return <MealPlanContainer />;
}
