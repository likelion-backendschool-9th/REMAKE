import type { Metadata } from "next";

import { PatternsContainer } from "./_components/PatternsContainer/PatternsContainer";

export const metadata: Metadata = {
  title: "나의 소비",
  robots: { index: false },
};

/** p3.4 나의 소비 */
export default function PatternsPage() {
  return <PatternsContainer />;
}
