import type { Metadata } from "next";

import { RecipesContainer } from "./_components/RecipesContainer/RecipesContainer";

export const metadata: Metadata = {
  title: "레시피",
  robots: { index: false },
};

/** p4.1 레시피 */
export default function SavedRecipesPage() {
  return <RecipesContainer live />;
}
