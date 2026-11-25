import type { Metadata } from "next";

import { ReceiptItemAddContainer } from "./_components/ReceiptItemAddContainer/ReceiptItemAddContainer";

export const metadata: Metadata = {
  title: "재료 추가",
  robots: { index: false },
};

/** p3.3.2 영수증 재료 추가 */
export default function ReceiptItemAddPage() {
  return <ReceiptItemAddContainer />;
}
