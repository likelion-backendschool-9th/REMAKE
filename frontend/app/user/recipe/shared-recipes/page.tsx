import type { Metadata } from "next";

import { RankingContainer } from "./_components/RankingContainer/RankingContainer";

export const metadata: Metadata = {
  title: "금주의 인기 레시피",
  robots: { index: false },
};

/** p4.1.4 금주의 인기 레시피 */
export default function SharedRecipesPage() {
  return <RankingContainer />;
}
