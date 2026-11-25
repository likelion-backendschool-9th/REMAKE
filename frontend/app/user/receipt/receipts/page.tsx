import type { Metadata } from "next";

import { ReceiptsContainer } from "./_components/ReceiptsContainer/ReceiptsContainer";

export const metadata: Metadata = {
  title: "영수증",
  robots: { index: false },
};

/** p3 영수증 */
export default function ReceiptsPage() {
  return <ReceiptsContainer />;
}
