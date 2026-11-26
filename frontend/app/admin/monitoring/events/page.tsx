import type { Metadata } from "next";

import { EventLogContainer } from "./_components/EventLogContainer/EventLogContainer";

export const metadata: Metadata = {
  title: "이벤트 로그",
  robots: { index: false },
};

/** a6 이벤트 로그 */
export default function AdminEventsPage() {
  return <EventLogContainer />;
}
