import type { Metadata } from "next";

import { CustomRecipeContainer } from "./_components/CustomRecipeContainer/CustomRecipeContainer";

export const metadata: Metadata = {
  title: "커스텀 레시피",
  robots: { index: false },
};

/** p4.1.2 커스텀 레시피. TODO: API 5(가구원 수)·API 12(냉장고 재료)를 붙여 defaults로 넘긴다 */
export default function CustomRecipePage() {
  return <CustomRecipeContainer />;
}
