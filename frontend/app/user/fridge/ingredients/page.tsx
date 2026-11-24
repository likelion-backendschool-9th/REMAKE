import type { Metadata } from "next";

import { FridgeContainer } from "./_components/FridgeContainer/FridgeContainer";

export const metadata: Metadata = {
  title: "냉장고",
  robots: { index: false },
};

/** p2 냉장고(메인) */
export default function FridgeIngredientsPage() {
  return <FridgeContainer />;
}
