import type { Metadata } from "next";

import { ReceiptItemEditContainer } from "./_components/ReceiptItemEditContainer/ReceiptItemEditContainer";

export const metadata: Metadata = {
  title: "재료 수정",
  robots: { index: false },
};

/** p3.3.1 영수증 재료 수정. TODO: Suspense 안에서 params.index를 읽고 OCR 결과 화면 상태에서 그 품목을 넘긴다 */
export default function ReceiptItemEditPage() {
  return <ReceiptItemEditContainer />;
}
