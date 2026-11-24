import type { Metadata } from "next";

import { ExcludedIngredientsContainer } from "./_components/ExcludedIngredientsContainer/ExcludedIngredientsContainer";

export const metadata: Metadata = {
  title: "알레르기 재료 제외",
  robots: { index: false },
};

/** p1.3.2 알레르기 재료 제외 */
export default function SettingExcludedIngredientsPage() {
  return <ExcludedIngredientsContainer />;
}
