import type { Metadata } from "next";

import { ReceiptDetailContainer } from "./_components/ReceiptDetailContainer/ReceiptDetailContainer";

export const metadata: Metadata = {
  title: "영수증",
  robots: { index: false },
};

/** p3.2 영수증 디테일. TODO: API 23을 붙일 때 Suspense 안에서 params.id를 읽어 넘긴다 */
export default function ReceiptDetailPage() {
  return <ReceiptDetailContainer />;
}
