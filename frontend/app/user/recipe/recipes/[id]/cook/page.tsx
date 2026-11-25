import type { Metadata } from "next";

import { CookContainer } from "./_components/CookContainer/CookContainer";

export const metadata: Metadata = {
  title: "재료 차감",
  robots: { index: false },
};

/** p4.1.1.2 레시피 사용 후 재료 차감. TODO: API 31을 붙일 때 Suspense 안에서 params.id를 읽어 넘긴다 */
export default function CookPage() {
  return <CookContainer />;
}
