import type { Metadata } from "next";

import { CustomRecipeContainer } from "./_components/CustomRecipeContainer/CustomRecipeContainer";

export const metadata: Metadata = {
  title: "커스텀 레시피",
  robots: { index: false },
};

/** p4.1.2 커스텀 레시피. 가구원 수(API 5)·냉장고 재료(API 12)는 live일 때 컨테이너가 직접 불러온다 */
export default function CustomRecipePage() {
  return <CustomRecipeContainer live />;
}
