import type { Metadata } from "next";

import { EditIngredientContainer } from "./_components/EditIngredientContainer/EditIngredientContainer";

export const metadata: Metadata = {
  title: "재료 수정",
  robots: { index: false },
};

/** p2.1 재료 수정. TODO: API 13을 붙일 때 Suspense 안에서 params.id를 읽어 넘긴다 */
export default function EditIngredientPage() {
  return <EditIngredientContainer />;
}
