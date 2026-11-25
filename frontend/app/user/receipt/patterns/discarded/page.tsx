import type { Metadata } from "next";

import { DiscardedContainer } from "./_components/DiscardedContainer/DiscardedContainer";

export const metadata: Metadata = {
  title: "버린 식재료",
  robots: { index: false },
};

/** p3.4.1 이번 달 버린 식재료. TODO: Suspense 안에서 searchParams.month를 읽어 그 달 값을 불러온다 */
export default function DiscardedPage() {
  return <DiscardedContainer />;
}
