import type { Metadata } from "next";

import { ReceiptResultContainer } from "./_components/ReceiptResultContainer/ReceiptResultContainer";

export const metadata: Metadata = {
  title: "영수증 재료",
  robots: { index: false },
};

/** p3.3 OCR 결과. TODO: 스캔 화면(p3.1)이 넘긴 OCR 결과를 화면 상태로 받아 그린다 */
export default function ReceiptResultPage() {
  return <ReceiptResultContainer />;
}
