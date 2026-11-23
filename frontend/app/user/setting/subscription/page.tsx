import type { Metadata } from "next";

import { SubscriptionContainer } from "./_components/SubscriptionContainer/SubscriptionContainer";

export const metadata: Metadata = {
  title: "구독 관리",
  robots: { index: false },
};

/** p1.3.4 구독 관리 */
export default function SubscriptionPage() {
  return <SubscriptionContainer />;
}
