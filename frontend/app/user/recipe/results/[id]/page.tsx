import type { Metadata } from "next";

import { RecipeResultContainer } from "./_components/RecipeResultContainer/RecipeResultContainer";

export const metadata: Metadata = {
  title: "레시피 생성",
  robots: { index: false },
};

/** p4.1.3 레시피 생성 결과. TODO: 생성 응답·API 27(남은 횟수)을 붙여 넘긴다 */
export default function RecipeResultPage() {
  return <RecipeResultContainer />;
}
