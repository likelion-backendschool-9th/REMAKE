import type { Metadata } from "next";

import { RecipeDetailContainer } from "./_components/RecipeDetailContainer/RecipeDetailContainer";

export const metadata: Metadata = {
  title: "레시피 상세",
  robots: { index: false },
};

/** p4.1.1 레시피 상세. TODO: API 30을 붙일 때 Suspense 안에서 params.id를 읽어 넘긴다 */
export default function RecipeDetailPage() {
  return <RecipeDetailContainer />;
}
