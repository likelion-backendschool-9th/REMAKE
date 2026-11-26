import type { Metadata } from "next";

import { FunnelContainer } from "./_components/FunnelContainer/FunnelContainer";

export const metadata: Metadata = {
  title: "퍼널",
  robots: { index: false },
};

/** a2 퍼널 */
export default function AdminFunnelsPage() {
  return <FunnelContainer />;
}
