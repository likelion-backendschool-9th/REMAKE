import type { Metadata } from "next";

import { AddIngredientContainer } from "./_components/AddIngredientContainer/AddIngredientContainer";

export const metadata: Metadata = {
  title: "재료 추가",
  robots: { index: false },
};

/** p2.2 재료 추가 */
export default function AddIngredientPage() {
  return <AddIngredientContainer />;
}
