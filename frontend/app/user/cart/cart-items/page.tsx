import type { Metadata } from "next";

import { CartContainer } from "./_components/CartContainer/CartContainer";

export const metadata: Metadata = {
  title: "장바구니",
  robots: { index: false },
};

/** p7 장바구니 */
export default function CartPage() {
  return <CartContainer />;
}
