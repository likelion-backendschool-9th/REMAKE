import type { Metadata } from "next";

import { ShareRecipeContainer } from "./_components/ShareRecipeContainer/ShareRecipeContainer";

export const metadata: Metadata = {
  title: "레시피 공유하기",
  robots: { index: false },
};

/** p4.1.1.1 레시피 공유 작성. TODO: Suspense 안에서 ?recipeId=를 읽어 API 30 값을 넘긴다 */
export default function ShareRecipePage() {
  return <ShareRecipeContainer />;
}
