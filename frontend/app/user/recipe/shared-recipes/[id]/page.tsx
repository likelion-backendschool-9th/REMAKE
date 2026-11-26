import type { Metadata } from "next";

import { SharedRecipeDetailContainer } from "./_components/SharedRecipeDetailContainer/SharedRecipeDetailContainer";

export const metadata: Metadata = {
  title: "공유 레시피",
  robots: { index: false },
};

/** p4.1.4.1 공유 레시피 상세. TODO: API 52를 붙일 때 Suspense 안에서 params.id를 읽어 넘긴다 */
export default function SharedRecipeDetailPage() {
  return <SharedRecipeDetailContainer />;
}
